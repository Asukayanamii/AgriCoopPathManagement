import random
import math
import os

# ===== K-means 聚类算法 =====
class KMeans:
    def __init__(self, n, m, q, space_cluster, deviation, iteration_count, xx, yy):
        self.q = q
        self.space_cluster = space_cluster
        self.deviation = deviation
        self.iteration_count = iteration_count
        self.tot = 0
        self.centers = []

        self.points = []
        for i in range(1, q + 1):
            self.points.append({'x': xx[i], 'y': yy[i], 'father': -1, 'meanid': 0, 'dis': float('inf')})

    def _dist2(self, a, b):
        return (self.points[a]['x'] - self.points[b]['x']) ** 2 + \
               (self.points[a]['y'] - self.points[b]['y']) ** 2

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
                    old_mid = self.points[i]['meanid']
                    if old_mid != self.points[cid]['meanid'] and old_mid != 0:
                        change += 1
                    self.points[i]['father'] = cid
                    self.points[i]['meanid'] = self.points[cid]['meanid']
                    self.points[i]['dis'] = d
        return change

    def _farthest_in_cluster(self, mid):
        best_d = -1
        best_i = self.centers[mid - 1]
        center = self.centers[mid - 1]
        for i in range(self.q):
            if self.points[i]['meanid'] == mid and i != center:
                d = self._dist2(center, i)
                if d > best_d:
                    best_d = d
                    best_i = i
        return best_i

    def _recompute_centers(self):
        sums = [[0, 0, 0] for _ in range(self.tot + 1)]
        for i in range(self.q):
            mid = self.points[i]['meanid']
            if mid > 0:
                sums[mid][0] += self.points[i]['x']
                sums[mid][1] += self.points[i]['y']
                sums[mid][2] += 1

        new_centers = []
        for mid in range(1, self.tot + 1):
            if sums[mid][2] == 0:
                new_centers.append(self.centers[mid - 1])
                continue
            best_d = float('inf')
            best_i = self.centers[mid - 1]
            avg_x = sums[mid][0] // sums[mid][2]
            avg_y = sums[mid][1] // sums[mid][2]
            for i in range(self.q):
                if self.points[i]['meanid'] == mid:
                    d = abs(self.points[i]['x'] - avg_x) + abs(self.points[i]['y'] - avg_y)
                    if d < best_d:
                        best_d = d
                        best_i = i
            new_centers.append(best_i)

        for mid in range(1, self.tot + 1):
            old_c = self.centers[mid - 1]
            new_c = new_centers[mid - 1]
            if old_c != new_c:
                self.points[old_c]['father'] = old_c
                self.points[old_c]['meanid'] = mid
                self.points[old_c]['dis'] = float('inf')

                self.points[new_c]['father'] = new_c
                self.points[new_c]['meanid'] = mid
                self.points[new_c]['dis'] = 0
                self.centers[mid - 1] = new_c

    def _cluster_sizes(self):
        sizes = [0] * (self.tot + 1)
        for i in range(self.q):
            mid = self.points[i]['meanid']
            if mid > 0:
                sizes[mid] += 1
        return sizes

    def _reset_dis(self):
        for i in range(self.q):
            if self.points[i]['father'] != i:
                self.points[i]['dis'] = float('inf')

    def run(self):
        random.seed()

        first = random.randint(0, self.q - 1)
        self._add_center(first)
        self.centers.append(first)
        self._assign(first)

        while True:
            sizes = self._cluster_sizes()
            if not sizes:
                break
            max_mid = max(range(1, self.tot + 1), key=lambda m: sizes[m])
            if sizes[max_mid] < self.space_cluster:
                break

            farthest = self._farthest_in_cluster(max_mid)
            if farthest == self.centers[max_mid - 1]:
                break

            self._add_center(farthest)
            self.centers.append(farthest)
            self._assign(farthest)

            for _ in range(self.iteration_count):
                self._recompute_centers()
                self._reset_dis()
                changes = 0
                for c in self.centers:
                    changes += self._assign(c)
                if changes <= self.deviation:
                    break

    def get_results(self):
        return self.tot, self.centers, self.points


def generate_k_means_test_data():
    q = 100
    random.seed(42)

    clusters_spec = [(30, 30, 35, 8), (70, 30, 35, 8), (50, 70, 30, 8)]
    xx = [0] * (q + 1)
    yy = [0] * (q + 1)
    idx = 1
    for cx, cy, n, spread in clusters_spec:
        for _ in range(n):
            xx[idx] = max(1, min(100, int(cx + random.gauss(0, spread))))
            yy[idx] = max(1, min(100, int(cy + random.gauss(0, spread))))
            idx += 1
    return (0, 0, q, 20, 3, 20, xx, yy)


def test_k_means():
    print("=" * 60)
    print("K-means 聚类算法测试")
    print("=" * 60)

    n, m, q, space_cluster, deviation, it_count, xx, yy = generate_k_means_test_data()
    km = KMeans(n, m, q, space_cluster, deviation, it_count, xx, yy)
    km.run()
    tot, centers, pts = km.get_results()

    print(f"\n  [结果] 共形成 {tot} 个聚类")

    cluster_data = {}
    for i in range(q):
        mid = pts[i]['meanid']
        if mid not in cluster_data:
            cluster_data[mid] = {'points': []}
        cluster_data[mid]['points'].append((pts[i]['x'], pts[i]['y']))

    for mid in sorted(cluster_data.keys()):
        cx, cy = pts[centers[mid - 1]]['x'], pts[centers[mid - 1]]['y']
        print(f"  聚类{mid}: 中心({cx},{cy}), 大小{len(cluster_data[mid]['points'])}")

    colors = ['#e74c3c', '#3498db', '#2ecc71', '#f39c12', '#9b59b6',
              '#1abc9c', '#e67e22', '#2c3e50', '#c0392b', '#16a085']

    svg = ['<svg viewBox="0 0 600 600" style="background:#f8f9fa;border-radius:8px">']
    for mid in sorted(cluster_data.keys()):
        c = colors[(mid - 1) % len(colors)]
        cx, cy = pts[centers[mid - 1]]['x'], pts[centers[mid - 1]]['y']
        svg.append(f'<circle cx="{cx*5}" cy="{cy*5}" r="10" fill="{c}" stroke="#fff" stroke-width="3"/>')
        svg.append(f'<text x="{cx*5}" y="{cy*5+3}" font-size="10" fill="white" text-anchor="middle" font-weight="bold">{mid}</text>')
        for px, py in cluster_data[mid]['points']:
            svg.append(f'<circle cx="{px*5}" cy="{py*5}" r="3" fill="{c}" opacity="0.6"/>')
    svg.append('</svg>')

    table_rows = ""
    for mid in sorted(cluster_data.keys()):
        c = colors[(mid - 1) % len(colors)]
        cx, cy = pts[centers[mid - 1]]['x'], pts[centers[mid - 1]]['y']
        pts_list = cluster_data[mid]['points']
        min_d = min(math.sqrt((px-cx)**2+(py-cy)**2) for px, py in pts_list) or 0
        max_d = max(math.sqrt((px-cx)**2+(py-cy)**2) for px, py in pts_list) or 0
        avg_d = sum(math.sqrt((px-cx)**2+(py-cy)**2) for px, py in pts_list)/len(pts_list) if pts_list else 0
        table_rows += f"""<tr>
            <td><span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:{c};margin-right:5px"></span>聚类{mid}</td>
            <td>({cx},{cy})</td><td>{len(pts_list)}</td><td>{min_d:.1f}</td><td>{max_d:.1f}</td><td>{avg_d:.1f}</td>
        </tr>"""

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head><meta charset="UTF-8">
<title>K-means 聚类算法 - 测试结果</title>
<style>
    body {{ font-family:'Segoe UI',Arial,sans-serif; padding:20px; background:#f0f2f5; }}
    .container {{ max-width:1200px; margin:0 auto; }}
    h1 {{ color:#2c3e50; border-bottom:3px solid #e74c3c; padding-bottom:10px; }}
    .card {{ background:white; border-radius:12px; padding:25px; margin:20px 0; box-shadow:0 2px 10px rgba(0,0,0,0.08); }}
    table {{ width:100%; border-collapse:collapse; margin-top:15px; }}
    th,td {{ padding:12px; text-align:left; border-bottom:1px solid #eee; }}
    th {{ background:#e74c3c; color:white; }}
    tr:hover {{ background:#f5f8fa; }}
    .info {{ background:#fef2f2; border-left:4px solid #e74c3c; padding:15px; border-radius:4px; }}
    .stats-grid {{ display:grid; grid-template-columns:repeat(4,1fr); gap:15px; }}
    .stat-card {{ padding:20px; border-radius:10px; text-align:center; color:white; }}
    .stat-card:nth-child(1) {{ background:linear-gradient(135deg,#667eea,#764ba2); }}
    .stat-card:nth-child(2) {{ background:linear-gradient(135deg,#f093fb,#f5576c); }}
    .stat-card:nth-child(3) {{ background:linear-gradient(135deg,#4facfe,#00f2fe); }}
    .stat-card:nth-child(4) {{ background:linear-gradient(135deg,#43e97b,#38f9d7); color:#333; }}
    .stat-card .num {{ font-size:32px; font-weight:bold; }}
    .stat-card .label {{ font-size:14px; opacity:0.9; }}
</style></head>
<body><div class="container">
    <h1>K-means 空间聚类算法 测试报告</h1>
    <div class="info"><strong>算法说明：</strong>K-means 聚类算法通过迭代分裂自适应确定聚类数量。从随机种子点开始，不断分裂节点数超过阈值的聚类，并重新计算聚类中心，直到所有聚类满足约束条件。</div>
    <div class="stats-grid">
        <div class="stat-card"><div class="num">{tot}</div><div class="label">聚类总数</div></div>
        <div class="stat-card"><div class="num">{q}</div><div class="label">数据点总数</div></div>
        <div class="stat-card"><div class="num">{space_cluster}</div><div class="label">容量阈值</div></div>
        <div class="stat-card"><div class="num">{deviation}</div><div class="label">收敛偏差</div></div>
    </div>
    <div class="card"><h2>聚类可视化</h2>{''.join(svg)}
        <div style="margin-top:12px;display:flex;gap:15px;flex-wrap:wrap">
            {' '.join(f'<span><span style="display:inline-block;width:12px;height:12px;border-radius:50%;background:{colors[i%len(colors)]};margin-right:3px"></span>聚类{i+1}</span>' for i in range(tot))}
        </div>
    </div>
    <div class="card"><h2>聚类详情</h2><table><thead><tr><th>聚类</th><th>中心</th><th>节点数</th><th>最小距离</th><th>最大距离</th><th>平均距离</th></tr></thead><tbody>{table_rows}</tbody></table></div>
    <div class="card"><h2>复杂度</h2><ul><li>分裂策略：选取聚类中最远的点</li><li>迭代收敛：变化 <={deviation} 或达最大 {it_count} 次</li><li>时间复杂度：O(I*K*Q)</li></ul></div>
</div></body></html>"""

    path = os.path.join("results", "k_means_result.html")
    with open(path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"\n[OK] K-means 测试报告已生成: {path}")
    return tot, cluster_data

if __name__ == "__main__":
    test_k_means()
