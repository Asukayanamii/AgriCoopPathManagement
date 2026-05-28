import random
import math
import os

# ===== 资源搜索算法（小车调度）=====
# resource[] = 小车（配送车），有位置、编号、状态（可用/忙碌）
# target = 需要服务的作业点
# 算法：找到距离目标最近且可用的 requir 辆小车

class ResourceSearch:
    def __init__(self, n, m, k, x, y, ids, state):
        self.n = n
        self.m = m
        self.k = k
        self.trolleys = [None]
        for i in range(1, k + 1):
            t = {'x': x[i], 'y': y[i], 'id': ids[i], 'state': state[i]}
            self.trolleys.append(t)
        self.trolleys[1:] = sorted(self.trolleys[1:], key=lambda r: r['id'])

    def _count_in_radius(self, tx, ty, r):
        s = 0
        for i in range(1, self.k + 1):
            t = self.trolleys[i]
            if t['state'] and abs(t['x'] - tx) <= r and abs(t['y'] - ty) <= r:
                s += 1
        return s

    def search(self, target_x, target_y, requir):
        """找到距离 target 最近的 requir 辆可用小车"""
        l, r = 0, min(self.n, self.m)
        best_cnt = float('inf')
        best_ids = []

        while l <= r:
            mid = (l + r) // 2
            cnt = self._count_in_radius(target_x, target_y, mid)
            if cnt >= requir and cnt < best_cnt:
                best_cnt = cnt
                best_ids = []
                for j in range(1, self.k + 1):
                    t = self.trolleys[j]
                    if t['state'] and abs(t['x'] - target_x) <= mid and abs(t['y'] - target_y) <= mid:
                        best_ids.append(t['id'])
                r = mid - 1
            elif cnt < requir:
                l = mid + 1
            else:
                r = mid - 1

        if best_cnt == float('inf'):
            return [-1]

        h = [(abs(target_x - self.trolleys[rid]['x']) + abs(target_y - self.trolleys[rid]['y']), rid) for rid in best_ids]
        h.sort(key=lambda x: x[0])
        return [requir] + [rid for _, rid in h[:requir]]


def generate_test_data():
    """生成测试场景：
       小车（资源）停靠在各区域的停车场
       作业请求（目标）是田间需要处理的位置
    """
    n, m = 100, 100
    k = 30  # 30辆小车
    random.seed(123)

    x = [0] * (k + 1)
    y = [0] * (k + 1)
    ids = [0] * (k + 1)
    state = [False] * (k + 1)

    # 小车停靠在 5 个停车场
    depots = [(20, 20), (80, 20), (50, 80), (30, 60), (70, 60)]
    for i in range(1, k + 1):
        cx, cy = random.choice(depots)
        x[i] = max(1, min(n, int(cx + random.gauss(0, 5))))
        y[i] = max(1, min(m, int(cy + random.gauss(0, 5))))
        ids[i] = i
        state[i] = random.random() > 0.2  # 80% 小车可用

    # 5个田间作业请求，每个需要若干辆小车
    q = 5
    target_x = [0, 30, 70, 50, 20, 80]
    target_y = [0, 30, 30, 70, 65, 65]
    requir = [0, 3, 4, 2, 3, 3]

    return n, m, k, x, y, ids, state, q, target_x, target_y, requir, depots


def test_resource_search():
    print("=" * 60)
    print("小车调度算法测试 (Resource Search)")
    print("=" * 60)

    n, m, k, x, y, ids, state, q, tx, ty, req, depots = generate_test_data()
    rs = ResourceSearch(n, m, k, x, y, ids, state)

    all_results = []
    for i in range(1, q + 1):
        res = rs.search(tx[i], ty[i], req[i])
        all_results.append(res)
        status = "OK" if res[0] != -1 else "FAIL"
        print(f"\n  作业点 {i}: 位置({tx[i]}, {ty[i]}), 需要 {req[i]} 辆小车")
        if res[0] == -1:
            print(f"    无法满足需求（可用小车不足）")
        else:
            print(f"    已调度 {res[0]} 辆小车, 编号: {res[1:]}")

    # SVG
    svg_parts = [f'<svg viewBox="0 0 600 600" xmlns="http://www.w3.org/2000/svg" style="background:#f8f9fa;border-radius:8px">']

    # 停车场区域
    for cx, cy in depots:
        svg_parts.append(f'<ellipse cx="{cx*6}" cy="{cy*6}" rx="50" ry="50" fill="#eef2ff" stroke="#c7d2fe" stroke-width="1" stroke-dasharray="5,5"/>')
        svg_parts.append(f'<text x="{cx*6-20}" y="{cy*6-55}" font-size="11" fill="#6366f1" font-weight="bold">停车场</text>')

    # 小车
    for i in range(1, k + 1):
        col = "#22c55e" if state[i] else "#ef4444"
        svg_parts.append(f'<rect x="{x[i]*6-5}" y="{y[i]*6-4}" width="10" height="8" rx="2" fill="{col}" opacity="0.8"/>')
        svg_parts.append(f'<text x="{x[i]*6-4}" y="{y[i]*6-8}" font-size="7" fill="#555">T{i}</text>')

    # 作业请求
    colors = ['#e74c3c', '#3498db', '#f39c12', '#9b59b6', '#1abc9c']
    for i in range(1, q + 1):
        c = colors[i-1]
        svg_parts.append(f'<polygon points="{tx[i]*6},{ty[i]*6-8} {tx[i]*6-6},{ty[i]*6+4} {tx[i]*6+6},{ty[i]*6+4}" fill="{c}" stroke="#fff" stroke-width="2"/>')
        svg_parts.append(f'<text x="{tx[i]*6+10}" y="{ty[i]*6+4}" font-size="12" fill="{c}" font-weight="bold">P{i}</text>')

    svg_parts.append('</svg>')

    legend = """
    <div style="margin-top:15px;display:flex;gap:20px;flex-wrap:wrap">
        <span><span style="display:inline-block;width:10px;height:8px;border-radius:2px;background:#22c55e;margin-right:5px"></span>可用小车</span>
        <span><span style="display:inline-block;width:10px;height:8px;border-radius:2px;background:#ef4444;margin-right:5px"></span>忙碌小车</span>
        <span><span style="display:inline-block;width:0;height:0;border-left:6px solid transparent;border-right:6px solid transparent;border-bottom:10px solid #e74c3c;margin-right:5px;display:inline-block"></span>作业请求点</span>
        <span><span style="display:inline-block;width:12px;height:12px;border-radius:50%;border:1px dashed #6366f1;margin-right:5px"></span>停车场区域</span>
    </div>
    """

    table_rows = ""
    for i in range(q):
        res = all_results[i]
        color = colors[i % len(colors)]
        if res[0] != -1:
            ids_str = ", ".join(f"T{r}" for r in res[1:])
            status = '<span style="color:#22c55e;font-weight:bold">已调度</span>'
            found = f"{res[0]} 辆"
        else:
            ids_str = "-"
            status = '<span style="color:#ef4444;font-weight:bold">无法满足</span>'
            found = "0"
        table_rows += f"""<tr>
            <td><span style="display:inline-block;width:12px;height:12px;background:{color};clip-path:polygon(50% 0%, 0% 100%, 100% 100%);margin-right:5px;vertical-align:middle"></span>P{i+1}</td>
            <td>({tx[i+1]}, {ty[i+1]})</td>
            <td>{req[i+1]}</td>
            <td>{found}</td>
            <td>{ids_str}</td>
            <td>{status}</td>
        </tr>"""

    trolley_rows = ""
    for i in range(1, k + 1):
        icon = '<span style="color:#22c55e">空闲</span>' if state[i] else '<span style="color:#ef4444">忙碌</span>'
        trolley_rows += f"<tr><td>T{i}</td><td>({x[i]}, {y[i]})</td><td>{icon}</td></tr>"

    html = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>小车调度算法 - 测试结果</title>
<style>
    body {{ font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f0f2f5; }}
    .container {{ max-width: 1200px; margin: 0 auto; }}
    h1 {{ color: #2c3e50; border-bottom: 3px solid #22c55e; padding-bottom: 10px; }}
    h2 {{ color: #34495e; margin-top: 30px; }}
    .card {{ background: white; border-radius: 12px; padding: 25px; margin: 20px 0; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }}
    table {{ width: 100%; border-collapse: collapse; margin-top: 15px; }}
    th, td {{ padding: 12px 15px; text-align: left; border-bottom: 1px solid #eee; }}
    th {{ background: #22c55e; color: white; }}
    tr:hover {{ background: #f5f8fa; }}
    .info {{ background: #f0fdf4; border-left: 4px solid #22c55e; padding: 15px; border-radius: 4px; margin: 15px 0; }}
    .stats-grid {{ display: grid; grid-template-columns: repeat(4, 1fr); gap: 15px; margin: 15px 0; }}
    .stat-card {{ padding: 20px; border-radius: 10px; text-align: center; color: white; }}
    .stat-card:nth-child(1) {{ background: linear-gradient(135deg, #22c55e, #16a34a); }}
    .stat-card:nth-child(2) {{ background: linear-gradient(135deg, #3b82f6, #1d4ed8); }}
    .stat-card:nth-child(3) {{ background: linear-gradient(135deg, #f59e0b, #d97706); }}
    .stat-card:nth-child(4) {{ background: linear-gradient(135deg, #ef4444, #dc2626); }}
    .stat-card .num {{ font-size: 32px; font-weight: bold; }}
    .stat-card .label {{ font-size: 14px; opacity: 0.9; }}
    .scroll-table {{ max-height: 300px; overflow-y: auto; }}
    pre {{ background: #1e293b; color: #e2e8f0; padding: 15px; border-radius: 8px; overflow-x: auto; }}
</style>
</head>
<body>
<div class="container">
    <h1>小车调度算法 测试报告</h1>
    <div class="info">
        <strong>算法说明：</strong>给定一批可用小车（资源）和作业请求点（目标），
        通过二分查找确定最小调度半径，找到满足需求数量的可用小车，
        按曼哈顿距离排序，调度最近的小车前往作业点。
    </div>

    <div class="stats-grid">
        <div class="stat-card"><div class="num">{k}</div><div class="label">小车总数</div></div>
        <div class="stat-card"><div class="num">{sum(1 for s in state[1:] if s)}</div><div class="label">可用小车</div></div>
        <div class="stat-card"><div class="num">{sum(1 for s in state[1:] if not s)}</div><div class="label">忙碌小车</div></div>
        <div class="stat-card"><div class="num">{q}</div><div class="label">作业请求数</div></div>
    </div>

    <div class="card">
        <h2>场景可视化</h2>
        {''.join(svg_parts)}
        {legend}
        <p style="margin-top:10px;color:#666;font-size:13px">
            <b>图例：</b>绿色方块=可用小车 | 红色方块=忙碌小车 | 三角形=作业请求点 | 虚线圆=停车场
        </p>
    </div>

    <div class="card">
        <h2>调度结果</h2>
        <table>
            <thead><tr><th>作业点</th><th>位置</th><th>需求小车数</th><th>调度数量</th><th>调度小车编号</th><th>状态</th></tr></thead>
            <tbody>
                {table_rows}
            </tbody>
        </table>
    </div>

    <div class="card">
        <h2>全部小车列表</h2>
        <div class="scroll-table">
            <table>
                <thead><tr><th>编号</th><th>位置</th><th>状态</th></tr></thead>
                <tbody>
                    {trolley_rows}
                </tbody>
            </table>
        </div>
    </div>

    <div class="card">
        <h2>算法复杂度</h2>
        <ul>
            <li><strong>二分查找半径:</strong> O(log(min(n, m)) * k)，k为小车数</li>
            <li><strong>曼哈顿距离排序:</strong> O(k log k)</li>
            <li><strong>总复杂度:</strong> O(log(n) * k + k log k)</li>
        </ul>
    </div>
</div>
</body>
</html>"""

    output_path = os.path.join("results", "resource_search_result.html")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"\n[OK] 小车调度测试报告已生成: {output_path}")
    return all_results

if __name__ == "__main__":
    test_resource_search()
