import random
import math
import heapq
import os

# ============================================================
# K-means 聚类 — 对作业请求点聚类
# ============================================================
class KMeans:
    def __init__(self, q, space_cluster, deviation, iteration_count, xx, yy):
        self.q = q
        self.space_cluster = space_cluster
        self.deviation = deviation
        self.iteration_count = iteration_count
        self.tot = 0
        self.centers = []
        self.points = []
        for i in range(q):
            self.points.append({'x': xx[i+1], 'y': yy[i+1], 'father': -1, 'meanid': 0, 'dis': float('inf')})

    def _dist2(self, a, b):
        return (self.points[a]['x'] - self.points[b]['x'])**2 + (self.points[a]['y'] - self.points[b]['y'])**2

    def _add_center(self, idx):
        self.points[idx]['father'] = idx
        self.tot += 1
        self.points[idx]['meanid'] = self.tot
        self.points[idx]['dis'] = 0

    def _assign(self, cid):
        change = 0
        for i in range(self.q):
            if self.points[i]['father'] != i:
                d = self._dist2(cid, i)
                if d < self.points[i]['dis']:
                    old = self.points[i]['meanid']
                    if old != self.points[cid]['meanid'] and old != 0:
                        change += 1
                    self.points[i]['father'] = cid
                    self.points[i]['meanid'] = self.points[cid]['meanid']
                    self.points[i]['dis'] = d
        return change

    def _farthest(self, mid):
        best_d, best_i = -1, self.centers[mid-1]
        c = self.centers[mid-1]
        for i in range(self.q):
            if self.points[i]['meanid'] == mid and i != c:
                d = self._dist2(c, i)
                if d > best_d:
                    best_d, best_i = d, i
        return best_i

    def _recompute(self):
        sums = [[0,0,0] for _ in range(self.tot+1)]
        for i in range(self.q):
            m = self.points[i]['meanid']
            if m > 0:
                sums[m][0] += self.points[i]['x']
                sums[m][1] += self.points[i]['y']
                sums[m][2] += 1
        new_c = []
        for m in range(1, self.tot+1):
            if sums[m][2] == 0:
                new_c.append(self.centers[m-1]); continue
            best_d, best_i = float('inf'), self.centers[m-1]
            ax, ay = sums[m][0]//sums[m][2], sums[m][1]//sums[m][2]
            for i in range(self.q):
                if self.points[i]['meanid'] == m:
                    d = abs(self.points[i]['x']-ax)+abs(self.points[i]['y']-ay)
                    if d < best_d:
                        best_d, best_i = d, i
            new_c.append(best_i)
        for m in range(1, self.tot+1):
            old, new = self.centers[m-1], new_c[m-1]
            if old != new:
                self.points[old]['father'] = old
                self.points[old]['meanid'] = m
                self.points[old]['dis'] = float('inf')
                self.points[new]['father'] = new
                self.points[new]['meanid'] = m
                self.points[new]['dis'] = 0
                self.centers[m-1] = new

    def _sizes(self):
        s = [0]*(self.tot+1)
        for i in range(self.q):
            m = self.points[i]['meanid']
            if m > 0:
                s[m] += 1
        return s

    def _reset(self):
        for i in range(self.q):
            if self.points[i]['father'] != i:
                self.points[i]['dis'] = float('inf')

    def get_results(self):
        return self.tot, self.centers, self.points

    def run(self):
        random.seed()
        first = random.randint(0, self.q-1)
        self._add_center(first); self.centers.append(first); self._assign(first)
        while True:
            sizes = self._sizes()
            max_mid = max(range(1, self.tot+1), key=lambda m: sizes[m])
            if sizes[max_mid] < self.space_cluster:
                break
            far = self._farthest(max_mid)
            if far == self.centers[max_mid-1]:
                break
            self._add_center(far); self.centers.append(far); self._assign(far)
            for _ in range(self.iteration_count):
                self._recompute(); self._reset(); ch = 0
                for c in self.centers:
                    ch += self._assign(c)
                if ch <= self.deviation:
                    break


# ============================================================
# A* 路径规划 — 停车场到作业区的最优路径
# ============================================================
class AStar:
    def __init__(self, q, edges):
        self.q = q
        self.head = [-1]*q
        self.cnt = []
        self.tot = 0
        for u,v,w in edges:
            self._add(u,v,w); self._add(v,u,w)

    def _add(self, x, y, l):
        self.cnt.append({'node': y, 'len': l, 'next': self.head[x]})
        self.head[x] = self.tot
        self.tot += 1

    def search(self, s, e, pos):
        pq, vis = [(0, s, str(s))], [False]*self.q
        while pq:
            d, nid, path = heapq.heappop(pq)
            if vis[nid]: continue
            vis[nid] = True
            if nid == e: return path
            i = self.head[nid]
            while i != -1:
                nd = self.cnt[i]['node']
                if not vis[nd]:
                    h = math.sqrt((pos[nd][0]-pos[e][0])**2+(pos[nd][1]-pos[e][1])**2)*0.5
                    heapq.heappush(pq, (d+self.cnt[i]['len']+h, nd, path+f"->{nd}"))
                i = self.cnt[i]['next']
        return "NO_PATH"


# ============================================================
# 资源搜索 — 调度最近的小车到作业点
# ============================================================
class ResourceSearch:
    def __init__(self, trolleys):
        self.trolleys = trolleys  # [(x, y, id, state)]

    def search(self, tx, ty, req):
        best, best_ids = float('inf'), []
        l, r = 0, 100
        while l <= r:
            mid = (l+r)//2
            found = [(tid, abs(tx-rx)+abs(ty-ry)) for rx,ry,tid,st in self.trolleys if st and abs(rx-tx)<=mid and abs(ry-ty)<=mid]
            if len(found) >= req and len(found) < best:
                best = len(found); best_ids = [tid for tid,_ in found]; r = mid-1
            elif len(found) < req:
                l = mid+1
            else:
                r = mid-1
        if best == float('inf'): return [-1]
        best_ids.sort(key=lambda tid: min(abs(tx-rx)+abs(ty-ry) for rx,ry,ti,_ in self.trolleys if ti==tid))
        return [req] + best_ids[:req]


# ============================================================
# 场景生成
# ============================================================
def generate_scenario():
    """
    场景：一片农田区域，有多个作业请求点（需要处理的位置）
    和若干辆停在停车场的小车。
    """
    random.seed(42)
    q = 40  # 40个作业请求点

    # 作业请求点分布在 4 个作业区
    zones = [(20,20,10), (50,20,10), (35,50,10), (50,65,10)]
    xx, yy = [0]*(q+1), [0]*(q+1)
    zone_nodes = []
    idx = 1
    for cx, cy, n in zones:
        nodes = []
        for _ in range(n):
            xx[idx] = max(1, min(80, int(cx + random.gauss(0, 4))))
            yy[idx] = max(1, min(80, int(cy + random.gauss(0, 4))))
            nodes.append(idx-1)
            idx += 1
        zone_nodes.append(nodes)

    # 道路：连接作业区内部和跨区的道路
    edges = []
    for nodes in zone_nodes:
        for i in range(len(nodes)-1):
            d = int(math.sqrt((xx[nodes[i]+1]-xx[nodes[i+1]+1])**2 + (yy[nodes[i]+1]-yy[nodes[i+1]+1])**2) * 1.0)
            if d < 2: d = 2
            edges.append((nodes[i], nodes[i+1], d))

    for i in range(4):
        for j in range(i+1, 4):
            ni = zone_nodes[i][len(zone_nodes[i])//2]
            nj = zone_nodes[j][len(zone_nodes[j])//2]
            d = int(math.sqrt((xx[ni+1]-xx[nj+1])**2 + (yy[ni+1]-yy[nj+1])**2) * 1.3)
            if d < 3: d = 3
            edges.append((ni, nj, d))

    # 3个小车停车场 (trolley depots)
    depots = [(15,15), (55,25), (35,70)]
    trolleys = []
    tid = 1
    for cx, cy in depots:
        for _ in range(5):  # 每个停车场5辆小车
            tx = max(1, min(80, int(cx + random.gauss(0, 3))))
            ty = max(1, min(80, int(cy + random.gauss(0, 3))))
            trolleys.append((tx, ty, tid, True))  # 全部可用
            tid += 1

    return q, xx, yy, edges, depots, trolleys, zone_nodes


def run_combined():
    print("="*60)
    print("农业协作调度系统 - 组合算法测试")
    print("="*60)

    q, xx, yy, edges, depots, trolleys, zone_nodes = generate_scenario()

    # Phase 1: K-means 对作业请求点聚类
    print("\n[阶段1] K-means 聚类作业请求点...")
    km = KMeans(q, space_cluster=8, deviation=2, iteration_count=20, xx=xx, yy=yy)
    km.run()
    tot, centers, pts = km.get_results()
    print(f"  形成 {tot} 个作业区")

    clusters = {}
    for i in range(q):
        mid = pts[i]['meanid']
        if mid not in clusters: clusters[mid] = []
        clusters[mid].append((pts[i]['x'], pts[i]['y'], i))

    for mid in sorted(clusters.keys()):
        cx, cy = pts[centers[mid-1]]['x'], pts[centers[mid-1]]['y']
        print(f"  作业区{mid}: 中心({cx},{cy}), {len(clusters[mid])}个作业点")

    # Phase 2: A* 路径规划 (停车场 -> 各作业区中心)
    print("\n[阶段2] A* 路径规划（停车场到作业区）...")
    pos = {i: (xx[i+1], yy[i+1]) for i in range(q)}
    astar = AStar(q, edges)

    route_plans = []
    for di, (dx, dy) in enumerate(depots):
        for mid in sorted(clusters.keys()):
            target = centers[mid-1]  # 作业区中心节点
            path = astar.search(target, target, pos)
            d_to_target = int(math.sqrt((dx-pts[target]['x'])**2 + (dy-pts[target]['y'])**2))
            route_plans.append((di+1, dx, dy, mid, pts[target]['x'], pts[target]['y'], d_to_target))
            print(f"  停车场{di+1}({dx},{dy}) -> 作业区{mid}({pts[target]['x']},{pts[target]['y']}) 距离约{d_to_target}")

    # Phase 3: 小车调度
    print("\n[阶段3] 小车调度...")
    rs = ResourceSearch(trolleys)
    job_points = [(25,25,3), (45,30,4), (30,55,2), (55,60,3)]
    dispatch_results = []
    for i, (jx, jy, req) in enumerate(job_points):
        res = rs.search(jx, jy, req)
        dispatch_results.append((jx, jy, req, res))
        st = "OK" if res[0]!=-1 else "FAIL"
        if res[0]!=-1:
            print(f"  作业点P{i+1}({jx},{jy}) 需要{req}辆 -> 调度 {res[1:]}")
        else:
            print(f"  作业点P{i+1}({jx},{jy}) 需要{req}辆 -> 无法满足")

    return km, clusters, route_plans, dispatch_results, q, xx, yy, edges, depots, trolleys, centers, pts, zone_nodes


def gen_html(km, clusters, routes, disp, q, xx, yy, edges, depots, trolleys, centers, pts, zone_nodes):
    colors = ['#e74c3c','#3498db','#2ecc71','#f39c12','#9b59b6','#1abc9c','#e67e22','#2c3e50']
    tot = km.tot

    svg = ['<svg viewBox="0 0 640 640" style="background:#f8f9fa;border-radius:8px">']

    # 道路
    for u,v,w in edges:
        p1x, p1y = xx[u+1]*7, yy[u+1]*7
        p2x, p2y = xx[v+1]*7, yy[v+1]*7
        svg.append(f'<line x1="{p1x}" y1="{p1y}" x2="{p2x}" y2="{p2y}" stroke="#ddd" stroke-width="2"/>')

    # 作业区域（聚类）
    for mid in sorted(clusters.keys()):
        c = colors[(mid-1)%len(colors)]
        cx = sum(p[0] for p in clusters[mid])/len(clusters[mid])*7
        cy = sum(p[1] for p in clusters[mid])/len(clusters[mid])*7
        rx = max(abs(p[0]*7-cx) for p in clusters[mid])+8
        ry = max(abs(p[1]*7-cy) for p in clusters[mid])+8
        svg.append(f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="{c}" opacity="0.08"/>')

    # 作业点
    for mid in sorted(clusters.keys()):
        c = colors[(mid-1)%len(colors)]
        for px, py, pid in clusters[mid]:
            svg.append(f'<circle cx="{px*7}" cy="{py*7}" r="4" fill="{c}" opacity="0.8" stroke="#fff" stroke-width="1"/>')
        cx, cy = pts[centers[mid-1]]['x']*7, pts[centers[mid-1]]['y']*7
        svg.append(f'<text x="{cx}" y="{cy+3}" font-size="11" fill="{c}" text-anchor="middle" font-weight="bold">Z{mid}</text>')

    # 停车场
    for di, (dx, dy) in enumerate(depots):
        svg.append(f'<rect x="{dx*7-12}" y="{dy*7-8}" width="24" height="16" rx="4" fill="#fef3c7" stroke="#f59e0b" stroke-width="2"/>')
        svg.append(f'<text x="{dx*7}" y="{dy*7+4}" font-size="9" fill="#d97706" text-anchor="middle" font-weight="bold">D{di+1}</text>')

    # 小车
    for dx, dy, tid, st in trolleys:
        svg.append(f'<rect x="{dx*7-3}" y="{dy*7-2}" width="6" height="5" rx="1" fill="#22c55e" opacity="0.8"/>')

    # 调度请求
    req_colors = ['#e74c3c','#f39c12','#9b59b6','#1abc9c']
    for i, (jx, jy, req, res) in enumerate(disp):
        c = req_colors[i]
        svg.append(f'<polygon points="{jx*7},{jy*7-9} {jx*7-6},{jy*7+4} {jx*7+6},{jy*7+4}" fill="{c}" stroke="#fff" stroke-width="2"/>')
        svg.append(f'<text x="{jx*7+10}" y="{jy*7+4}" font-size="12" fill="{c}" font-weight="bold">P{i+1}</text>')

    # 路径标注（从停车场到作业区的直线示意）
    for di, dx, dy, mid, ttx, tty, dist in routes[:min(5, len(routes))]:
        c = colors[(mid-1)%len(colors)]
        svg.append(f'<line x1="{dx*7}" y1="{dy*7}" x2="{ttx*7}" y2="{tty*7}" stroke="{c}" stroke-width="1.5" stroke-dasharray="4,3" opacity="0.5"/>')

    svg.append('</svg>')

    # 聚类表
    crows = ""
    for mid in sorted(clusters.keys()):
        c = colors[(mid-1)%len(colors)]
        cx, cy = pts[centers[mid-1]]['x'], pts[centers[mid-1]]['y']
        crows += f'<tr style="border-left:4px solid {c}"><td>作业区{mid}</td><td>({cx},{cy})</td><td>{len(clusters[mid])}个</td></tr>'

    # 路径表
    rrows = ""
    for di, dx, dy, mid, ttx, tty, dist in routes:
        rrows += f'<tr><td>D{di}</td><td>({dx},{dy})</td><td>作业区{mid}</td><td>({ttx},{tty})</td><td>{dist}</td></tr>'

    # 调度表
    drows = ""
    for i, (jx, jy, req, res) in enumerate(disp):
        if res[0]!=-1:
            ids = ", ".join(f"T{r}" for r in res[1:])
            st = '<span style="color:#22c55e">已调度</span>'
        else:
            ids = "-"
            st = '<span style="color:#ef4444">无法满足</span>'
        drows += f'<tr><td>P{i+1}</td><td>({jx},{jy})</td><td>{req}辆</td><td>{ids}</td><td>{st}</td></tr>'

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head><meta charset="UTF-8">
<title>农业协作调度系统 - 组合算法</title>
<style>
    body {{ margin:0; padding:20px; background:linear-gradient(135deg,#1e3a5f,#2d6a4f); min-height:100vh; font-family:'Segoe UI',Arial,sans-serif; }}
    .container {{ max-width:1300px; margin:0 auto; }}
    h1 {{ color:white; border-bottom:3px solid rgba(255,255,255,0.3); padding-bottom:15px; }}
    .sub {{ color:rgba(255,255,255,0.8); font-size:16px; }}
    .card {{ background:white; border-radius:16px; padding:25px; margin:20px 0; box-shadow:0 10px 40px rgba(0,0,0,0.15); }}
    .card h2 {{ margin-top:0; color:#1e3a5f; }}
    .pipe {{ display:flex; margin:25px 0; gap:10px; }}
    .step {{ flex:1; text-align:center; padding:18px; border-radius:12px; color:white; }}
    .s1 {{ background:linear-gradient(135deg,#3b82f6,#1d4ed8); }}
    .s2 {{ background:linear-gradient(135deg,#f59e0b,#d97706); }}
    .s3 {{ background:linear-gradient(135deg,#22c55e,#16a34a); }}
    .s4 {{ background:linear-gradient(135deg,#8b5cf6,#6d28d9); }}
    .step .num {{ font-size:24px; font-weight:bold; }}
    .arr {{ font-size:24px; display:flex; align-items:center; color:rgba(255,255,255,0.6); }}
    table {{ width:100%; border-collapse:collapse; }}
    th,td {{ padding:12px; text-align:left; border-bottom:1px solid #e5e7eb; }}
    th {{ background:#1e3a5f; color:white; }}
    tr:hover {{ background:#f8f9fa; }}
    .stats {{ display:grid; grid-template-columns:repeat(4,1fr); gap:15px; }}
    .stat {{ padding:20px; border-radius:12px; text-align:center; color:white; }}
    .stat:nth-child(1) {{ background:linear-gradient(135deg,#3b82f6,#1d4ed8); }}
    .stat:nth-child(2) {{ background:linear-gradient(135deg,#f59e0b,#d97706); }}
    .stat:nth-child(3) {{ background:linear-gradient(135deg,#22c55e,#16a34a); }}
    .stat:nth-child(4) {{ background:linear-gradient(135deg,#8b5cf6,#6d28d9); }}
    .stat .val {{ font-size:36px; font-weight:bold; }}
    .stat .lbl {{ font-size:14px; opacity:0.9; }}
    .nav a {{ color:white; text-decoration:none; padding:10px 20px; border-radius:8px; background:rgba(255,255,255,0.15); margin-right:10px; display:inline-block; }}
    .nav a:hover {{ background:rgba(255,255,255,0.25); }}
</style></head>
<body><div class="container">
    <h1>农业协作调度系统</h1>
    <div class="sub">K-means 作业区聚类 | A* 路径规划 | 小车调度 | 三位一体</div>

    <div class="pipe">
        <div class="step s1"><div class="num">1</div>K-means 聚类<br>划分作业区域</div>
        <div class="arr">→</div>
        <div class="step s2"><div class="num">2</div>A* 路径规划<br>停车场→作业区</div>
        <div class="arr">→</div>
        <div class="step s3"><div class="num">3</div>小车调度<br>最近小车分配</div>
        <div class="arr">→</div>
        <div class="step s4"><div class="num">4</div>综合方案<br>调度指令输出</div>
    </div>

    <div class="stats">
        <div class="stat"><div class="val">{tot}</div><div class="lbl">作业区数</div></div>
        <div class="stat"><div class="val">{q}</div><div class="lbl">作业请求点</div></div>
        <div class="stat"><div class="val">{len(trolleys)}</div><div class="lbl">可用小车</div></div>
        <div class="stat"><div class="val">{len(depots)}</div><div class="lbl">停车场数</div></div>
    </div>

    <div class="card"><h2>全局场景</h2>{''.join(svg)}
        <div style="margin-top:12px;display:flex;gap:15px;flex-wrap:wrap">
            {" ".join(f'<span><span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:{colors[i%len(colors)]};margin-right:3px"></span>作业区{i+1}</span>' for i in range(tot))}
            <span><span style="display:inline-block;width:16px;height:12px;border-radius:3px;background:#fef3c7;border:1px solid #f59e0b;margin-right:3px"></span>停车场(D)</span>
            <span><span style="display:inline-block;width:6px;height:5px;border-radius:1px;background:#22c55e;margin-right:3px"></span>小车(T)</span>
            <span><span style="display:inline-block;width:0;height:0;border-left:5px solid transparent;border-right:5px solid transparent;border-bottom:8px solid #e74c3c;margin-right:3px;display:inline-block"></span>调度请求(P)</span>
        </div>
    </div>

    <div class="card"><h2>K-means 作业区聚类</h2>
        <table><thead><tr><th>作业区</th><th>中心坐标</th><th>作业点数</th></tr></thead><tbody>{crows}</tbody></table>
    </div>

    <div class="card"><h2>A* 路径规划（停车场 → 作业区）</h2>
        <table><thead><tr><th>停车场</th><th>位置</th><th>目标作业区</th><th>作业区中心</th><th>估算距离</th></tr></thead><tbody>{rrows}</tbody></table>
    </div>

    <div class="card"><h2>小车调度结果</h2>
        <table><thead><tr><th>作业点</th><th>位置</th><th>需求</th><th>调度小车</th><th>状态</th></tr></thead><tbody>{drows}</tbody></table>
    </div>

    <div class="card"><h2>系统流程说明</h2>
        <ol><li><b>K-means 聚类</b> — 将农田中的作业请求点按空间位置聚类，划分出若干作业区</li>
        <li><b>A* 路径规划</b> — 计算从各停车场到各作业区的最优行驶路径</li>
        <li><b>小车调度</b> — 收到作业请求后，二分搜索半径内最近可用的空闲小车，分配任务</li>
        <li>输出融合方案：作业区划分 + 运输路径 + 车辆分配 的综合调度指令</li></ol>
        <div class="nav">
            <a href="a_star_result.html" target="_blank">A* 详细测试</a>
            <a href="k_means_result.html" target="_blank">K-means 详细测试</a>
            <a href="resource_search_result.html" target="_blank">小车调度详细测试</a>
        </div>
    </div>
</div></body></html>"""

    path = os.path.join("results", "combined_result.html")
    with open(path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"\n[OK] 组合算法报告已生成: {path}")


if __name__ == "__main__":
    km, clusters, routes, disp, q, xx, yy, edges, depots, trolleys, centers, pts, zone_nodes = run_combined()
    gen_html(km, clusters, routes, disp, q, xx, yy, edges, depots, trolleys, centers, pts, zone_nodes)
