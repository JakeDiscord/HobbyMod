#!/usr/bin/env python3
"""Check the handbook's navigable links, real recipes, icons and blank Astronomy."""
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'common/src/main/resources'
BOOK = RES / 'assets/hobbymod/patchouli_books/hobbies/en_us'
categories = {p.stem: json.loads(p.read_text()) for p in (BOOK / 'categories').glob('*.json')}
entries = {p.relative_to(BOOK / 'entries').with_suffix('').as_posix(): json.loads(p.read_text()) for p in (BOOK / 'entries').rglob('*.json')}
recipes = {p.stem: json.loads(p.read_text()) for p in (RES / 'data/hobbymod/recipe').glob('*.json')}
refs = set()
errors = []

for key, entry in entries.items():
    cat = entry['category'].removeprefix('hobbymod:')
    if cat not in categories:
        errors.append(f'{key}: missing category {cat}')
    for obj in [entry, *entry['pages']]:
        icon = obj.get('icon', '')
        if icon.startswith('hobbymod:') and not (RES / 'assets/hobbymod/models/item' / (icon.split(':')[1] + '.json')).exists():
            errors.append(f'{key}: missing icon model {icon}')
    for page in entry['pages']:
        for field in ('recipe', 'recipe2'):
            if field in page:
                rid = page[field].removeprefix('hobbymod:')
                refs.add(rid)
                if rid not in recipes:
                    errors.append(f'{key}: missing recipe {rid}')
        body = page.get('text', '')
        for target in re.findall(r'\$\(l:([^)]*)\)', body):
            target = target.removeprefix('hobbymod:').split('#')[0]
            if target not in entries and target not in categories:
                errors.append(f'{key}: broken link {target}')
        visible = re.sub(r'\$\([^)]*\)', '', body)
        if len(visible) > 350:
            errors.append(f'{key}: text page has {len(visible)} characters (limit 350)')

assert categories['astronomy']['description'] == ''
assert not any(e['category'] == 'hobbymod:astronomy' for e in entries.values())
expected = set(recipes) - {'astronomy_journal', 'observatory_telescope', 'telescope', 'marble_sculpture'}
missing = expected - refs
if missing:
    errors.append(f'Crafting recipes absent from book: {sorted(missing)}')
assert recipes['marble_sculpture']['type'] == 'minecraft:stonecutting'
assert recipes['hobby_handbook']['result']['components']['patchouli:book'] == 'hobbymod:hobbies'
if errors:
    raise SystemExit('\n'.join(errors))
print(f'Validated {len(entries)} entries, {len(refs)} recipe references, all links and icons; Astronomy is blank.')
