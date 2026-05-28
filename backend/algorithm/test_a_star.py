import heapq
import math
import random
import os

# ===== A* 算法实现 (改编自 A.cpp) =====
class AStar:
    def __init__(self, q, n, m, xx, yy, ww, cen_node, fa, mean, nodex, nodey, Len):
        self.q = q
        self.n = n
        self.m = m
        self.a = [None] * (q + 1)
        for i in range(1, q + 1):
            class Node: pass
            node = Node()
            node.x = xx[i]; node.y = yy[i]; node.w = ww[i]
            node.meanid = mean[i]; node.father = fa[i]
            self.a[i] = node

        self.head = [-1] * (q + 1)
        self.cnt = []
        self.tot = 0
        for i in range(1, n + 1):
            self._add_line(nodex[i], nodey[i], Len[i])
            self._add_line(nodey[i], nodex[i], Len[i])

        self.cen = [None] * (m + 1)
        for i in range(1, m + 1):
            class Cen: pass
            c = Cen()
            c.id = cen_node[i]
            c.dis = [float('inf')] * (q + 1)
            self.cen[i] = c

        for i in range(1, m + 1):
            self._dijkstra(self.cen[i].id)

    def _add_line(self, x, y, l):
        entry = {'node': y, 'len': l, 'next': self.head[x]}
        self.cnt.append(entry)
        self.head[x] = self.tot
        self.tot += 1

    def _dijkstra(self, k):
        idx = None
        for i in range(1, self.m + 1):
            if self.cen[i].id == k:
                idx = i
                break
        self.cen[idx].dis[k] = 0
        pq = [(0, k)]
        while pq:
            dist, now = heapq.heappop(pq)
            if dist > self.cen[idx].dis[now]:
                continue
            e = self.head[now]
            while e != -1:
                nd = self.cnt[e]['node']
                ndis = dist + self.cnt[e]['len']
                if ndis < self.cen[idx].dis[nd]:
                    self.cen[idx].dis[nd] = ndis
                    heapq.heappush(pq, (ndis, nd))
                e = self.cnt[e]['next']

    def _conclude_standard_value(self, x, y):
        value = -1
        for i in range(1, self.m + 1):
            val = abs(self.cen[i].dis[x] - self.cen[i].dis[y])
            if val > value:
                value = val
        return value

    def heuristic_search(self, start_node, end_node):
        open_list = []
        b = [False] * (self.q + 1)
        sv = self._conclude_standard_value(start_node, end_node)
        heapq.heappush(open_list, (sv, start_node, ""))
        b[start_node] = True
        while open_list:
            sv, nid, path = heapq.heappop(open_list)
            if nid == end_node:
                return path
            e = self.head[nid]
            while e != -1:
                new_id = self.cnt[e]['node']
                if not b[new_id]:
                    b[new_id] = True
                    new_sv = sv - self._conclude_standard_value(nid, end_node) + self.cnt[e]['len'] + self._conclude_standard_value(new_id, end_node)
                    new_path = path + f"->{new_id}"
                    heapq.heappush(open_list, (new_sv, new_id, new_path))
                e = self.cnt[e]['next']
        return "NO_PATH"

def generate_a_star_test_data():
    """生成A*测试数据 - 一个网格地图"""
    q = 20
    n = 30
    m = 3

    xx = [0] * (q + 1)
    yy = [0] * (q + 1)
    ww = [0] * (q + 1)
    fa = [0] * (q + 1)
    mean = [0] * (q + 1)
    cen_node = [0, 1, 6, 12]

    nodes_pos = {
        1: (10, 10), 2: (20, 10), 3: (30, 10), 4: (10, 20), 5: (20, 20),
        6: (30, 20), 7: (10, 30), 8: (20, 30), 9: (30, 30), 10: (40, 10),
        11: (40, 20), 12: (40, 30), 13: (50, 10), 14: (50, 20), 15: (50, 30),
        16: (15, 15), 17: (25, 15), 18: (15, 25), 19: (25, 25), 20: (35, 15)
    }

    for i in range(1, q + 1):
        xx[i], yy[i] = nodes_pos[i]
        ww[i] = random.randint(1, 5)
        fa[i] = 0
        if i <= 5:
            mean[i] = 1
        elif i <= 12:
            mean[i] = 2
        else:
            mean[i] = 3

    edges = [
        (1,2,10),(2,3,10),(1,4,12),(2,5,12),(3,6,12),
        (4,5,10),(5,6,10),(4,7,12),(5,8,12),(6,9,12),
        (7,8,10),(8,9,10),(3,10,12),(6,11,12),(9,12,12),
        (10,11,10),(11,12,10),(10,13,12),(11,14,12),(12,15,12),
        (13,14,10),(14,15,10),(16,1,5),(16,2,8),(16,4,8),
        (17,2,5),(17,3,8),(17,5,8),(18,4,5),(18,7,8),(18,5,8)
    ]

    nodex = [0] * (n + 1)
    nodey = [0] * (n + 1)
    Len = [0] * (n + 1)
    for i in range(1, n + 1):
        nodex[i], nodey[i], Len[i] = edges[i-1]

    return q, n, m, xx, yy, ww, cen_node, fa, mean, nodex, nodey, Len, nodes_pos

def test_a_star():
    print("=" * 60)
    print("A* 路径搜索算法测试")
    print("=" * 60)

    q, n, m, xx, yy, ww, cen_node, fa, mean, nodex, nodey, Len, nodes_pos = generate_a_star_test_data()
    astar = AStar(q, n, m, xx, yy, ww, cen_node, fa, mean, nodex, nodey, Len)

    test_cases = [(1, 15), (2, 14), (4, 13), (7, 10), (16, 15)]
    results = []
    html_rows = ""

    for s, e in test_cases:
        path = astar.heuristic_search(s, e)
        path_len = 0
        path_nodes = [s]
        if path and path != "NO_PATH":
            parts = path.split("->")
            for p in parts:
                if p:
                    path_nodes.append(int(p))
        results.append((s, e, path, path_nodes))
        path_str = " -> ".join(str(n) for n in path_nodes)
        print(f"  起点 {s} -> 终点 {e}: 路径 = {path_str}")
        if path != "NO_PATH" and path:
            for i in range(len(path_nodes) - 1):
                for j in range(1, n + 1):
                    if (nodex[j] == path_nodes[i] and nodey[j] == path_nodes[i+1]) or \
                       (nodex[j] == path_nodes[i+1] and nodey[j] == path_nodes[i]):
                        path_len += Len[j]
                        break
        print(f"  路径长度: {path_len}")

        html_rows += f"""<tr>
            <td>{s}</td>
            <td>{e}</td>
            <td>{path_str}</td>
            <td>{path_len}</td>
        </tr>"""

    svg_parts = []
    svg_parts.append(f'<svg viewBox="0 0 600 450" xmlns="http://www.w3.org/2000/svg" style="background:#f8f9fa;border-radius:8px">')

    for i in range(1, n + 1):
        x1, y1 = nodes_pos[nodex[i]]
        x2, y2 = nodes_pos[nodey[i]]
        svg_parts.append(f'<line x1="{x1*12}" y1="{y1*12}" x2="{x2*12}" y2="{y2*12}" stroke="#ccc" stroke-width="2"/>')
        midx = (x1 + x2) / 2 * 12
        midy = (y1 + y2) / 2 * 12
        svg_parts.append(f'<text x="{midx}" y="{midy-3}" font-size="9" fill="#999">{Len[i]}</text>')

    colors = ['#e74c3c', '#2ecc71', '#3498db', '#f39c12']
    for idx, (s, e, path, path_nodes) in enumerate(results):
        for i in range(len(path_nodes) - 1):
            x1, y1 = nodes_pos[path_nodes[i]]
            x2, y2 = nodes_pos[path_nodes[i+1]]
            svg_parts.append(f'<line x1="{x1*12}" y1="{y1*12}" x2="{x2*12}" y2="{y2*12}" stroke="{colors[idx%len(colors)]}" stroke-width="3" stroke-dasharray="5,3"/>')

    for i in range(1, q + 1):
        x, y = nodes_pos[i]
        color = '#3498db'
        if i == 1: color = '#e74c3c'
        if i == 15: color = '#9b59b6'
        svg_parts.append(f'<circle cx="{x*12}" cy="{y*12}" r="6" fill="{color}" stroke="#fff" stroke-width="2"/>')
        svg_parts.append(f'<text x="{x*12+8}" y="{y*12+4}" font-size="11" fill="#333">{i}</text>')

    svg_parts.append('</svg>')

    # 图例
    legend = """
    <div style="margin-top:15px;display:flex;gap:20px;flex-wrap:wrap">
        <span><span style="display:inline-block;width:16px;height:3px;background:#e74c3c;margin-right:5px"></span>路径1 (1->15)</span>
        <span><span style="display:inline-block;width:16px;height:3px;background:#2ecc71;margin-right:5px"></span>路径2 (2->14)</span>
        <span><span style="display:inline-block;width:16px;height:3px;background:#3498db;margin-right:5px"></span>路径3 (4->13)</span>
        <span><span style="display:inline-block;width:16px;height:3px;background:#f39c12;margin-right:5px"></span>路径4 (7->10)</span>
    </div>
    """

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>A* 启发式路径搜索算法 - 测试结果</title>
<style>
    body {{ font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f0f2f5; }}
    .container {{ max-width: 1200px; margin: 0 auto; }}
    h1 {{ color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }}
    h2 {{ color: #34495e; margin-top: 30px; }}
    .card {{ background: white; border-radius: 12px; padding: 25px; margin: 20px 0; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }}
    table {{ width: 100%; border-collapse: collapse; margin-top: 15px; }}
    th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; }}
    th {{ background: #3498db; color: white; }}
    tr:hover {{ background: #f5f8fa; }}
    .info {{ background: #eaf2f8; border-left: 4px solid #3498db; padding: 15px; border-radius: 4px; margin: 15px 0; }}
    .badge {{ display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; }}
    .badge-blue {{ background: #dbeafe; color: #1e40af; }}
    pre {{ background: #1e293b; color: #e2e8f0; padding: 15px; border-radius: 8px; overflow-x: auto; }}
</style>
</head>
<body>
<div class="container">
    <h1>🧭 A* 启发式路径搜索算法 测试报告</h1>
    <div class="info">
        <strong>算法说明：</strong>A*（A-Star）启发式搜索算法，结合Dijkstra最短路径预处理和启发式评估函数，
        在图中搜索从起点到终点的最优路径。使用最大距离差作为启发函数，保证搜索的方向性和效率。
    </div>

    <div class="card">
        <h2>📋 测试参数</h2>
        <table>
            <tr><td>节点数 (q)</td><td>{q}</td></tr>
            <tr><td>边数 (n)</td><td>{n}</td></tr>
            <tr><td>聚类中心数 (m)</td><td>{m}</td></tr>
            <tr><td>测试路径数</td><td>{len(test_cases)}</td></tr>
            <tr><td>中心节点</td><td>{cen_node[1:]}</td></tr>
        </table>
    </div>

    <div class="card">
        <h2>🗺️ 地图与搜索路径可视化</h2>
        {''.join(svg_parts)}
        {legend}
    </div>

    <div class="card">
        <h2>📊 路径搜索结果</h2>
        <table>
            <thead><tr><th>起点</th><th>终点</th><th>路径</th><th>路径长度</th></tr></thead>
            <tbody>
                {html_rows}
            </tbody>
        </table>
    </div>

    <div class="card">
        <h2>⚙️ 算法复杂度</h2>
        <ul>
            <li><strong>预处理 (Dijkstra):</strong> O(m * (q log q + n))，m为中心点数</li>
            <li><strong>启发式搜索:</strong> 最坏 O(q log q)，实际远小于此</li>
            <li><strong>启发函数:</strong> O(m) 计算最大距离差</li>
        </ul>
    </div>
</div>
</body>
</html>"""

    output_path = os.path.join("results", "a_star_result.html")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"\n[OK] A* 测试报告已生成: {output_path}")
    return results

if __name__ == "__main__":
    test_a_star()
