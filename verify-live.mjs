import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';

const base = 'https://comic.baby';
const checks = [];
function get(path) {
  const body = execFileSync('curl.exe', ['--noproxy', '*', '--fail', '--silent', '--show-error', '--location', '--max-time', '40', '--retry', '2', '--retry-all-errors', '--retry-delay', '1',
    '-H', 'X-Platform: h5', '-H', 'X-Locale: zh-CN', '-H', 'X-Client-Capabilities: img-aes-cbc-v1', base + path], { maxBuffer: 16 * 1024 * 1024 });
  return JSON.parse(body.toString('utf8'));
}
function record(name) { checks.push(name); console.log('PASS: ' + name); }
function validWork(w) {
  assert.equal(typeof w.id, 'string');
  assert.ok(w.id.length > 0);
  assert.equal(typeof w.title, 'string');
  assert.ok(w.title.length > 0);
  if (w.coverImageUrl) assert.equal(new URL(w.coverImageUrl).protocol, 'https:');
  for (const l of [...(w.categories || []), ...(w.tags || [])]) assert.equal(typeof l.name, 'string');
}
const popular = get('/api/catalog/works?type=COMIC&hasChapters=true&limit=24&offset=0&sort=popular');
assert.equal(popular.items.length, 24);
assert.ok(popular.total > 24);
popular.items.forEach(validWork);
const next = get('/api/catalog/works?type=COMIC&hasChapters=true&limit=24&offset=24&sort=popular');
assert.ok(!next.items.some(w => popular.items.some(p => p.id === w.id)));
record('Popular list, required fields, and distinct second page');
const updated1 = get('/api/catalog/updates?page=1&pageSize=24');
const updated2 = get('/api/catalog/updates?page=2&pageSize=24');
assert.ok(updated1.nextCursor);
assert.equal(updated1.items.length, 24);
updated1.items.forEach(i => validWork(i.work));
assert.notEqual(updated1.items[0].work.id, updated2.items[0].work.id);
record('Latest updates and page-number pagination');
const selected = popular.items[0];
const search = get('/api/catalog/search?' + new URLSearchParams({q: selected.serialCode, page: '1', pageSize: '24', sort: 'popular'}));
assert.ok(search.items.some(w => w.id === selected.id));
record('JM identifier search');
const keyword = get('/api/catalog/search?' + new URLSearchParams({q: selected.title, page: '1', pageSize: '24', sort: 'popular'}));
assert.ok(keyword.items.some(w => w.id === selected.id));
record('Chinese keyword search');
const none = get('/api/catalog/search?q=zzzz_no_such_comic_92f463cf&page=1&pageSize=24&sort=popular');
assert.ok(Array.isArray(none.items));
assert.equal(typeof none.total, 'number');
none.items.forEach(validWork);
record('Unmatched query response (website may return fallback recommendations)');
const tag = selected.tags?.[0]?.name;
if (tag) {
  const tagged = get('/api/catalog/works?' + new URLSearchParams({type: 'COMIC', hasChapters: 'true', limit: '24', offset: '0', sort: 'popular', tag}));
  assert.ok(tagged.items.length);
  assert.ok(tagged.total < popular.total);
  assert.ok(tagged.items.some(w => w.id === selected.id));
  tagged.items.forEach(validWork);
  record('Tag filter narrows the catalog and retains the known matching work');
}
const detail = get('/api/catalog/works/' + selected.id);
validWork(detail);
assert.ok(detail.chapters.length > 0);
for (const c of detail.chapters) {
  assert.equal(typeof c.id, 'string');
  assert.equal(typeof c.title, 'string');
  assert.equal(typeof c.chapterNumber, 'number');
  if (c.publishedAt) assert.ok(Number.isFinite(Date.parse(c.publishedAt)));
}
record('Details, chapter identifiers, numbers and timestamps');
const chapter = [...detail.chapters].sort((a, b) => a.chapterNumber - b.chapterNumber)[0];
const reader = get('/api/assets/chapters/' + chapter.id + '/read');
assert.notEqual(reader.access?.allowed, false);
assert.equal(reader.pages.length, chapter.pageCount);
assert.deepEqual(reader.pages.map(p => p.index), Array.from({length: reader.pages.length}, (_, i) => i));
for (const p of reader.pages) {
  assert.equal(new URL(p.imageUrl).protocol, 'https:');
  assert.ok(Number.isInteger(p.descrambleSegments || 0));
}
record('Complete public chapter and sequential page list');
const report = { checkedAt: new Date().toISOString(), baseUrl: base, checks, pageCount: reader.pages.length, limitations: ['No Android device was connected; in-app behavior has not been tested.', 'Authenticated/member-only chapters are not supported.'] };
writeFileSync(new URL('./verification.json', import.meta.url), JSON.stringify(report, null, 2) + '\n');
