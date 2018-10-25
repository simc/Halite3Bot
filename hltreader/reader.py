import json
import zstd
import aiohttp
import asyncio

session = None

c: aiohttp.ClientResponse

sizes = [32, 40, 48, 56, 64]
data = {}

async def fetch(url):
    async with session.get(url) as response:
        return await response.text()


async def fetchBytes(url):
    async with session.get(url) as response:
        return await response.read()


async def loadLeaderboardUsers():
    leaderboard = await fetch('https://api.2018.halite.io/v1/api/leaderboard?offset=0&limit=50')
    return [str(user['user_id']) for user in json.loads(leaderboard)]


async def loadRecentlyWonMatches(user):
    matches_str = await fetch(
        'https://api.2018.halite.io/v1/api/user/' + user + '/match?order_by=desc,time_played&offset=0&limit=10')
    matches = json.loads(matches_str)

    return [(
        str(match['game_id']),
        [stat['player_id'] == 1 for stat in match['stats']['player_statistics'] if stat['rank'] == 1][0],
        len(match['players'])
    ) for match in matches if match['players'][user]['rank'] == 1]


async def loadReplay(match):
    replay = await fetchBytes('https://api.2018.halite.io/v1/api/user/0/match/' + match + '/replay')
    return zstd.decompress(replay).decode('utf-8')


def findData(replay_str, player_id):
    replay = json.loads(replay_str)
    map_size = replay['GAME_CONSTANTS']['DEFAULT_MAP_HEIGHT']
    frame_id = 0
    max_spawn = 0
    for frame in replay['full_frames']:
        frame_id += 1
        if True in (event['type'] == 'spawn' and event['owner_id'] == player_id for event in frame['events']):
            max_spawn = frame_id

    return (map_size, max_spawn)


async def loadMatchData(match, player_id, num_players):
    replay = await loadReplay(match)
    (map_size, max_spawn) = findData(replay, player_id)
    (curr_max_spawn, curr_num) = data[map_size][num_players]
    data[map_size][num_players] = (curr_max_spawn + max_spawn, curr_num + 1)
    print('finished match #' + match)


async def loadUserData(user):
    won_matches = await loadRecentlyWonMatches(user)
    tasks = [loadMatchData(match, player_id, num_players) for (match, player_id, num_players) in won_matches]
    if len(tasks) > 0:
        await asyncio.wait(tasks)
    print('finished user #' + user)


async def main():
    global session
    async with aiohttp.ClientSession(connector=aiohttp.TCPConnector(verify_ssl=False)) as sess:
        session = sess

        for size in sizes:
            data[size] = {2: (0, 0), 4: (0, 0)}

        users = await loadLeaderboardUsers()
        tasks = [loadUserData(user) for user in users]
        await asyncio.wait(tasks)

        final_data = {}
        for size, p in data.items():
            entry = {}
            for players, dat in p.items():
                entry[players] = int(dat[0] / dat[1])
            final_data[size] = entry

        print(json.dumps(final_data, indent=4))


loop = asyncio.get_event_loop()
loop.run_until_complete(main())
