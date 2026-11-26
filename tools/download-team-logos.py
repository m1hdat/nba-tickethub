"""Download presentation-only NBA marks; geometry/court assets are unchanged."""
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen
from concurrent.futures import ThreadPoolExecutor
from datetime import date

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'app/src/main/assets/logos'
TEAMS = dict(atl='atl', bos='bos', bkn='bkn', cha='cha', chi='chi', cle='cle',
             dal='dal', den='den', det='det', gsw='gs', hou='hou', ind='ind',
             lac='lac', lal='lal', mem='mem', mia='mia', mil='mil', min='min',
             nop='no', nyk='ny', okc='okc', orl='orl', phi='phi', phx='phx',
             por='por', sac='sac', sas='sa', tor='tor', uta='utah', was='wsh')

def download(item):
    team, remote = item
    url = f'https://a.espncdn.com/i/teamlogos/nba/500/{remote}.png'
    with urlopen(url, timeout=30) as response:
        data = response.read()
    if not data.startswith(b'\x89PNG\r\n\x1a\n'):
        raise ValueError(f'Not PNG: {team}')
    (DEST / f'{team}.png').write_bytes(data)
    return dict(teamId=team, asset=f'logos/{team}.png', url=url,
                retrievedOn=date.today().isoformat(), sha256=hashlib.sha256(data).hexdigest(),
                usage='NBA/team trademarks; educational UI identification only. No open license claimed.')

if __name__ == '__main__':
    DEST.mkdir(parents=True, exist_ok=True)
    with ThreadPoolExecutor(max_workers=6) as pool:
        result = list(pool.map(download, TEAMS.items()))
    (DEST / 'SOURCES.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf8')
    print(f'Downloaded and checked {len(result)} PNG logos; sources and hashes recorded.')
