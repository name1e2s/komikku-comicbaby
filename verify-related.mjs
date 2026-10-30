import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';

const base = 'https://comic.baby';
function get(path) {
  const bytes = execFileSync('curl.exe', ['--noproxy', '*', '--fail', '--silent', '--show-error', '--location', '--max-time', '30', '--retry', '2', '--retry-all-errors', '--retry-delay', '1',
    '-H', 'X-Platform: h5', '-H', 'X-Locale: zh-CN', '-H', 'X-Client-Capabilities: img-aes-cbc-v1', base + path], { maxBuffer: 16 * 1024 * 1024 });
  return JSON.parse(bytes.toString('utf8'));
}
const workIds = ['gf6h0qua3q2v5aa4dmt2s6iz', 'cmorfwjgkuhihjionhi4og58j', 'cmorh3kflqwdojion9du15n5p'];
const checks = [];
for (const id of workIds) {
  const entries = get('/api/recommend/related/' + id + '?limit=18');
  assert.ok(Array.isArray(entries));
  assert.ok(entries.length > 0 && entries.length <= 18);
  const displayed = [];
  const titles = new Set();
  for (const { work } of entries) {
    assert.ok(work && typeof work.id === 'string' && work.id);
    assert.ok(typeof work.title === 'string' && work.title.trim());
    assert.equal(new URL(work.coverImageUrl).protocol, 'https:');
    for (const category of work.categories || []) assert.equal(typeof category.name, 'string');
    if (work.id === id || titles.has(work.title.trim())) continue;
    titles.add(work.title.trim());
    displayed.push(work);
    if (displayed.length === 12) break;
  }
  assert.ok(displayed.length > 0 && displayed.length <= 12);
  const detail = get('/api/catalog/works/' + displayed[0].id);
  assert.equal(detail.id, displayed[0].id);
  assert.ok(Array.isArray(detail.chapters) && detail.chapters.length > 0);
  checks.push({ workId: id, received: entries.length, displayed: displayed.length, firstRecommendationOpens: true });
  console.log(`PASS: ${id}: ${entries.length} recommendations, ${displayed.length} displayed, detail/chapters valid`);
}
// Updates shares WorkEntry with recommendations; validate the existing response shape too.
const updates = get('/api/catalog/updates?page=1&pageSize=24');
assert.ok(updates.items.length > 0);
assert.ok(updates.items.every(entry => typeof entry.work?.id === 'string' && typeof entry.work?.title === 'string'));
console.log('PASS: shared WorkEntry structure remains valid for latest updates');
writeFileSync(new URL('./related-verification.json', import.meta.url), JSON.stringify({ checkedAt: new Date().toISOString(), version: '1.6.2', endpoint: '/api/recommend/related/{workId}?limit=18', checks, latestUpdatesShapeValid: true, limitation: 'API and build validation only; no connected Android device.' }, null, 2) + '\n');
