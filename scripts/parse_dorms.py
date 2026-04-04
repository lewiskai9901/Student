import json

data = json.load(open('/tmp/pt.json'))['data']

def find(nodes):
    for n in nodes:
        name = n.get('placeName', '')
        nid = n['id']
        if name.find('\u5bbf\u820d') >= 0:  # 宿舍
            ch = n.get('children') or []
            rooms = 0
            for f in ch:
                fr = f.get('children') or []
                rooms += len(fr)
            print('BLDG id=%s name=%s floors=%d rooms=%d' % (nid, name, len(ch), rooms))
            for f in ch:
                fid = f['id']
                fn = f.get('placeName', '')
                fr = f.get('children') or []
                print('  FL id=%s name=%s rooms=%d' % (fid, fn, len(fr)))
        else:
            find(n.get('children') or [])

find(data)
