<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
	<title>Wordle Benchmark</title>
	<meta charset="UTF-8">
	<style>
		body { background-color: black; color: #d7dadc; font-family: 'Segoe UI', Arial, sans-serif; margin: 0; }
		header { background-color: #1A43BF; padding: 14px 24px; display: flex; align-items: baseline; gap: 16px; }
		header h1 { margin: 0; font-size: 1.4em; color: white; letter-spacing: 2px; }
		header a { color: #c9d4f5; font-size: 0.85em; text-decoration: none; }
		header a:hover { color: white; }
		main { max-width: 1040px; margin: 0 auto; padding: 24px; }
		h2 { font-size: 1.0em; color: #818384; text-transform: uppercase; letter-spacing: 1px;
		     margin: 28px 0 12px 0; }

		/* form */
		#form { display: flex; gap: 18px; flex-wrap: wrap; align-items: flex-end;
		        border: 1px solid #2a2a2c; border-radius: 8px; padding: 16px; }
		.field { display: flex; flex-direction: column; gap: 5px; }
		.field label { font-size: 0.75em; color: #818384; text-transform: uppercase; letter-spacing: 1px; }
		input[type=text], input[type=number], select {
			background: #1a1a1b; border: 1px solid #3a3a3c; color: #d7dadc;
			border-radius: 4px; padding: 8px 10px; font-size: 0.95em; }
		input[type=text] { width: 90px; text-transform: lowercase; }
		input[type=number] { width: 80px; }
		select { min-width: 130px; }
		.chk { display: flex; align-items: center; gap: 6px; padding-bottom: 9px;
		       font-size: 0.9em; color: #9fa3a6; }
		button { background: #1A43BF; color: white; border: none; border-radius: 5px;
		         padding: 10px 20px; font-size: 0.95em; cursor: pointer; }
		button:hover { background: #2a55d8; }
		button:disabled { background: #2a2a2c; color: #565758; cursor: default; }
		#runMsg { font-size: 0.9em; color: #9fa3a6; min-height: 1.2em; margin-top: 10px; }
		#runMsg.err { color: #cc4444; }

		/* histogram */
		#histo { display: flex; gap: 26px; align-items: flex-end; padding: 18px 8px 0 8px;
		         min-height: 230px; }
		.bucket { display: flex; flex-direction: column; align-items: center; gap: 6px; }
		.bars { display: flex; gap: 4px; align-items: flex-end; height: 190px; }
		.bar { width: 20px; border-radius: 3px 3px 0 0; transition: height 0.4s ease; position: relative; }
		.bar .pct { position: absolute; top: -17px; left: 50%; transform: translateX(-50%);
		            font-size: 0.62em; color: #818384; white-space: nowrap; }
		.blabel { font-size: 0.8em; color: #818384; }
		#legend { display: flex; gap: 18px; flex-wrap: wrap; margin-top: 14px; font-size: 0.82em; }
		.lg { display: flex; align-items: center; gap: 7px; color: #9fa3a6; }
		.dot { width: 12px; height: 12px; border-radius: 3px; }

		/* runs table */
		table { border-collapse: collapse; width: 100%; font-size: 0.88em; }
		th { text-align: left; color: #818384; font-weight: normal; text-transform: uppercase;
		     font-size: 0.78em; letter-spacing: 1px; padding: 8px 10px; border-bottom: 1px solid #2a2a2c; }
		td { padding: 8px 10px; border-bottom: 1px solid #1d1d1f; }
		tr:hover td { background: #131314; }
		td.num, th.num { text-align: right; }
		.cfg { color: #9fa3a6; }
		.best { color: #538d4e; font-weight: bold; }
	</style>
</head>
<body id="benchmarkPage">
	<header>
		<h1>BENCHMARK</h1>
		<a href=".">&larr; home</a>
	</header>
	<main>
		<h2>new run</h2>
		<div id="form">
			<div class="field"><label>opener</label>
				<input type="text" id="opener" value="salet" maxlength="5" spellcheck="false"></div>
			<div class="chk"><input type="checkbox" id="hardMode"><label for="hardMode">hard mode</label></div>
			<div class="field"><label>guess pool</label>
				<select id="guessPool">
					<option value="FULL" selected>full (12,947)</option>
					<option value="ANSWERS_ONLY">answers only (2,309)</option>
				</select></div>
			<div class="field"><label>tiebreak</label>
				<select id="tiebreak">
					<option value="PREFER_CANDIDATE" selected>prefer candidate</option>
					<option value="FIRST_BY_INDEX">first by index</option>
				</select></div>
			<div class="field"><label>scope</label>
				<select id="scope">
					<option value="full" selected>full list (2,309)</option>
					<option value="sample">sample…</option>
				</select></div>
			<div class="field" id="sampleFields" style="display:none"><label>size / seed</label>
				<div style="display:flex;gap:6px">
					<input type="number" id="sampleSize" value="300" min="10" max="2309">
					<input type="number" id="seed" value="42">
				</div></div>
			<button id="runBtn">Run benchmark</button>
		</div>
		<div id="runMsg"></div>

		<h2>guess distribution <span style="text-transform:none">(% of answers — tick rows below to compare up to 3)</span></h2>
		<div id="histo"></div>
		<div id="legend"></div>

		<h2>runs</h2>
		<table>
			<thead><tr>
				<th></th><th>when</th><th>config</th><th class="num">answers</th>
				<th class="num">win %</th><th class="num">avg</th><th class="num">fails</th>
				<th class="num">ms</th>
			</tr></thead>
			<tbody id="runsBody"></tbody>
		</table>
	</main>

	<script>
	(function () {
		var COLORS = ['#538d4e', '#1A43BF', '#b59f3b'];
		var allRuns = [];
		var selected = [];

		var scopeSel = document.getElementById('scope');
		scopeSel.addEventListener('change', function () {
			document.getElementById('sampleFields').style.display =
				scopeSel.value === 'sample' ? 'block' : 'none';
		});

		function fmtPct(x) { return (100 * x).toFixed(2) + '%'; }

		function runById(id) {
			for (var i = 0; i < allRuns.length; i++) {
				if (allRuns[i].id === id) { return allRuns[i]; }
			}
			return null;
		}

		function renderHisto() {
			var histo = document.getElementById('histo');
			var legend = document.getElementById('legend');
			var runs = [];
			for (var s = 0; s < selected.length; s++) {
				var r = runById(selected[s]);
				if (r) { runs.push(r); }
			}
			if (runs.length === 0) {
				histo.innerHTML = '<span style="color:#565758;font-size:0.85em">no run selected</span>';
				legend.innerHTML = '';
				return;
			}
			// percentage scale: max bucket share across selected runs
			var maxPct = 0.01;
			for (var i = 0; i < runs.length; i++) {
				for (var b = 0; b < 6; b++) {
					maxPct = Math.max(maxPct, runs[i].dist[b] / runs[i].answerCount);
				}
				maxPct = Math.max(maxPct, runs[i].fails / runs[i].answerCount);
			}
			var html = '';
			var labels = ['1', '2', '3', '4', '5', '6', 'fail'];
			for (var b2 = 0; b2 < 7; b2++) {
				html += '<div class="bucket"><div class="bars">';
				for (var i2 = 0; i2 < runs.length; i2++) {
					var count = b2 < 6 ? runs[i2].dist[b2] : runs[i2].fails;
					var pct = count / runs[i2].answerCount;
					var h = Math.round(185 * pct / maxPct);
					if (count > 0 && h < 3) { h = 3; }
					html += '<div class="bar" style="height:' + h + 'px;background:' + COLORS[i2] + '">'
						+ (count > 0 ? '<span class="pct">' + (100 * pct).toFixed(1) + '</span>' : '')
						+ '</div>';
				}
				html += '</div><div class="blabel">' + labels[b2] + '</div></div>';
			}
			histo.innerHTML = html;
			var lg = '';
			for (var i3 = 0; i3 < runs.length; i3++) {
				lg += '<div class="lg"><span class="dot" style="background:' + COLORS[i3] + '"></span>'
					+ runs[i3].configName + ' — avg ' + runs[i3].avgGuesses.toFixed(4)
					+ ', ' + fmtPct(runs[i3].winRate) + ' (' + runs[i3].answerCount + ' answers, '
					+ runs[i3].createdAt + ')</div>';
			}
			legend.innerHTML = lg;
		}

		function renderTable() {
			var body = document.getElementById('runsBody');
			var bestAvg = null;
			for (var i = 0; i < allRuns.length; i++) {
				if (allRuns[i].answerCount === 2309
						&& (bestAvg === null || allRuns[i].avgGuesses < bestAvg)) {
					bestAvg = allRuns[i].avgGuesses;
				}
			}
			var html = '';
			for (var j = 0; j < allRuns.length; j++) {
				var r = allRuns[j];
				var checked = selected.indexOf(r.id) >= 0 ? ' checked' : '';
				var avgCls = (r.answerCount === 2309 && r.avgGuesses === bestAvg) ? ' class="num best"' : ' class="num"';
				html += '<tr>'
					+ '<td><input type="checkbox" data-id="' + r.id + '"' + checked + '></td>'
					+ '<td>' + r.createdAt + '</td>'
					+ '<td class="cfg">' + r.configName + '</td>'
					+ '<td class="num">' + r.answerCount.toLocaleString() + '</td>'
					+ '<td class="num">' + fmtPct(r.winRate) + '</td>'
					+ '<td' + avgCls + '>' + r.avgGuesses.toFixed(4) + '</td>'
					+ '<td class="num">' + r.fails + '</td>'
					+ '<td class="num">' + r.durationMs.toLocaleString() + '</td>'
					+ '</tr>';
			}
			body.innerHTML = html;
			var boxes = body.querySelectorAll('input[type=checkbox]');
			for (var k = 0; k < boxes.length; k++) {
				boxes[k].addEventListener('change', function () {
					var id = parseInt(this.getAttribute('data-id'), 10);
					if (this.checked) {
						if (selected.length >= 3) { this.checked = false; return; }
						selected.push(id);
					} else {
						var idx = selected.indexOf(id);
						if (idx >= 0) { selected.splice(idx, 1); }
					}
					renderHisto();
				});
			}
		}

		function refresh(selectNewest) {
			return fetch('benchmark/runs')
				.then(function (r) { return r.json(); })
				.then(function (list) {
					allRuns = list;
					if (selectNewest && list.length > 0) {
						if (selected.indexOf(list[0].id) < 0) { selected.unshift(list[0].id); }
						while (selected.length > 3) { selected.pop(); }
					}
					renderTable();
					renderHisto();
				});
		}

		var runBtn = document.getElementById('runBtn');
		var runMsg = document.getElementById('runMsg');
		runBtn.addEventListener('click', function () {
			runBtn.disabled = true;
			runMsg.className = '';
			runMsg.textContent = 'running…';
			var body = {
				opener: document.getElementById('opener').value.trim(),
				hardMode: document.getElementById('hardMode').checked,
				guessPool: document.getElementById('guessPool').value,
				tiebreak: document.getElementById('tiebreak').value
			};
			if (scopeSel.value === 'sample') {
				body.sampleSize = parseInt(document.getElementById('sampleSize').value, 10) || 300;
				body.seed = parseInt(document.getElementById('seed').value, 10) || 42;
			}
			fetch('benchmark/run', {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: JSON.stringify(body)
			}).then(function (r) {
				if (!r.ok) { throw new Error('HTTP ' + r.status); }
				return r.json();
			}).then(function (resp) {
				if (resp.error) {
					runMsg.className = 'err';
					runMsg.textContent = resp.error;
					return;
				}
				var run = resp.run;
				runMsg.textContent = 'done: ' + run.configName + ' — avg ' + run.avgGuesses.toFixed(4)
					+ ', ' + fmtPct(run.winRate) + ' in ' + run.durationMs.toLocaleString() + ' ms';
				return refresh(true);
			}).catch(function (e) {
				runMsg.className = 'err';
				runMsg.textContent = 'run failed: ' + e.message;
			}).then(function () {
				runBtn.disabled = false;
			});
		});

		refresh(true);
	})();
	</script>
</body>
</html>
