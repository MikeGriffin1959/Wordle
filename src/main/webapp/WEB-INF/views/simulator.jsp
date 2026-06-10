<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
	<title>Wordle Simulator</title>
	<meta charset="UTF-8">
	<style>
		body { background-color: black; color: #d7dadc; font-family: 'Segoe UI', Arial, sans-serif; margin: 0; }
		header { background-color: #1A43BF; padding: 14px 24px; display: flex; align-items: baseline; gap: 16px; }
		header h1 { margin: 0; font-size: 1.4em; color: white; letter-spacing: 2px; }
		header a { color: #c9d4f5; font-size: 0.85em; text-decoration: none; }
		header a:hover { color: white; }
		main { display: flex; flex-wrap: wrap; gap: 36px; padding: 28px; justify-content: center; }

		/* board */
		#board { display: grid; grid-template-rows: repeat(6, 62px); gap: 6px; }
		.row { display: grid; grid-template-columns: repeat(5, 62px); gap: 6px; }
		.tile { width: 62px; height: 62px; border: 2px solid #3a3a3c; box-sizing: border-box;
		        display: flex; align-items: center; justify-content: center;
		        font-size: 2em; font-weight: bold; text-transform: uppercase; color: #d7dadc;
		        transition: transform 0.12s ease; }
		.tile.pop { transform: scale(1.12); border-color: #565758; }
		.tile.flipping { transform: rotateX(90deg); transition: transform 0.14s ease-in; }
		.tile.G { background-color: #538d4e; border-color: #538d4e; }
		.tile.Y { background-color: #b59f3b; border-color: #b59f3b; }
		.tile.B { background-color: #3a3a3c; border-color: #3a3a3c; }
		#status { margin-top: 14px; min-height: 1.4em; font-size: 1.05em; letter-spacing: 1px; text-align: center; }
		#status.win { color: #538d4e; }
		#status.loss { color: #cc4444; }

		/* readout panel */
		#panel { width: 460px; max-width: 95vw; }
		.bigcount { font-size: 2.6em; font-weight: bold; color: white; }
		.bigcount small { font-size: 0.4em; color: #818384; font-weight: normal; margin-left: 8px; }
		#candBarWrap { background: #1a1a1b; border: 1px solid #3a3a3c; height: 14px; border-radius: 7px;
		               overflow: hidden; margin: 10px 0 22px 0; }
		#candBar { background: linear-gradient(90deg, #1A43BF, #538d4e); height: 100%; width: 100%;
		           transition: width 0.6s ease; }
		.turnRow { border: 1px solid #2a2a2c; border-radius: 6px; padding: 9px 12px; margin-bottom: 8px;
		           display: none; }
		.turnRow.shown { display: block; }
		.turnHead { display: flex; align-items: center; gap: 10px; }
		.turnWord { font-weight: bold; text-transform: uppercase; letter-spacing: 2px; width: 90px; }
		.mini { display: inline-block; width: 11px; height: 11px; margin-right: 2px; border-radius: 2px; }
		.mini.G { background: #538d4e; } .mini.Y { background: #b59f3b; } .mini.B { background: #3a3a3c; }
		.bits { margin-left: auto; font-size: 0.82em; color: #9fa3a6; text-align: right; }
		.bits b { color: #d7dadc; }
		.sample { margin-top: 6px; font-size: 0.78em; color: #565758; }
		.sample span { color: #818384; }

		/* controls */
		#controls { margin-top: 6px; padding-top: 16px; border-top: 1px solid #2a2a2c;
		            display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
		button { background: #1A43BF; color: white; border: none; border-radius: 5px;
		         padding: 9px 16px; font-size: 0.95em; cursor: pointer; }
		button:hover { background: #2a55d8; }
		button.secondary { background: #3a3a3c; }
		button.secondary:hover { background: #4a4a4c; }
		label { font-size: 0.85em; color: #9fa3a6; display: flex; align-items: center; gap: 6px; }
		input[type=range] { width: 110px; accent-color: #1A43BF; }
		input[type=text] { background: #1a1a1b; border: 1px solid #3a3a3c; color: #d7dadc;
		                   border-radius: 4px; padding: 6px 8px; width: 90px; }
	</style>
</head>
<body id="simulatorPage">
	<header>
		<h1>SIMULATOR</h1>
		<a href=".">&larr; home</a>
	</header>
	<main>
		<div>
			<div id="board"></div>
			<div id="status"></div>
		</div>
		<div id="panel">
			<div class="bigcount"><span id="candCount">2309</span><small>candidates remaining</small></div>
			<div id="candBarWrap"><div id="candBar"></div></div>
			<div id="turns"></div>
			<div id="controls">
				<button id="newBtn">New game</button>
				<button id="pauseBtn" class="secondary">Pause</button>
				<label>speed <input type="range" id="speed" min="0.5" max="4" step="0.5" value="1">
					<span id="speedVal">1x</span></label>
				<label><input type="checkbox" id="loop" checked> loop</label>
				<label>seed <input type="text" id="seedBox" placeholder="random"></label>
			</div>
		</div>
	</main>

	<script>
	(function () {
		var TOTAL = 2309;
		var LOG_TOTAL = Math.log(TOTAL + 1);
		var state = { token: 0, paused: false };

		var board = document.getElementById('board');
		var statusEl = document.getElementById('status');
		var turnsEl = document.getElementById('turns');
		var candCount = document.getElementById('candCount');
		var candBar = document.getElementById('candBar');
		var speedSlider = document.getElementById('speed');
		var speedVal = document.getElementById('speedVal');
		var pauseBtn = document.getElementById('pauseBtn');

		function speed() { return parseFloat(speedSlider.value) || 1; }
		speedSlider.addEventListener('input', function () { speedVal.textContent = speed() + 'x'; });

		pauseBtn.addEventListener('click', function () {
			state.paused = !state.paused;
			pauseBtn.textContent = state.paused ? 'Play' : 'Pause';
		});

		document.getElementById('newBtn').addEventListener('click', function () { newGame(); });

		function buildBoard() {
			board.innerHTML = '';
			for (var r = 0; r < 6; r++) {
				var row = document.createElement('div');
				row.className = 'row';
				for (var c = 0; c < 5; c++) {
					var t = document.createElement('div');
					t.className = 'tile';
					row.appendChild(t);
				}
				board.appendChild(row);
			}
		}

		function tile(r, c) { return board.children[r].children[c]; }

		// pause/cancel-aware delay; reacts to live speed changes
		function delay(ms, token) {
			return new Promise(function (resolve) {
				var remaining = ms;
				var iv = setInterval(function () {
					if (token !== state.token) { clearInterval(iv); resolve(); return; }
					if (!state.paused) { remaining -= 30 * speed(); }
					if (remaining <= 0) { clearInterval(iv); resolve(); }
				}, 30);
			});
		}

		function setCandidates(n) {
			candCount.textContent = n.toLocaleString();
			var pct = Math.max(2, 100 * Math.log(n + 1) / LOG_TOTAL);
			candBar.style.width = pct + '%';
		}

		function miniTiles(pattern) {
			var html = '';
			for (var i = 0; i < 5; i++) { html += '<span class="mini ' + pattern[i] + '"></span>'; }
			return html;
		}

		function addTurnRow(turn) {
			var div = document.createElement('div');
			div.className = 'turnRow shown';
			var html = '<div class="turnHead"><span class="turnWord">' + turn.word + '</span>'
				+ '<span>' + miniTiles(turn.pattern) + '</span>'
				+ '<span class="bits">expected <b>' + turn.expectedBits.toFixed(2) + '</b> bits<br>'
				+ 'gained <b>' + turn.bitsGained.toFixed(2) + '</b> &rarr; <b>'
				+ turn.candidatesAfter.toLocaleString() + '</b> left</span></div>';
			if (turn.candidatesAfter > 0 && turn.candidatesAfter <= 50) {
				var sample = turn.remainingSample.join(', ');
				if (turn.candidatesAfter > turn.remainingSample.length) { sample += ', …'; }
				html += '<div class="sample">remaining: <span>' + sample + '</span></div>';
			}
			div.innerHTML = html;
			turnsEl.appendChild(div);
		}

		async function playTurn(turn, rowIdx, token) {
			// letters pop in
			for (var c = 0; c < 5; c++) {
				if (token !== state.token) return;
				var t = tile(rowIdx, c);
				t.textContent = turn.word[c];
				t.classList.add('pop');
				(function (el) { setTimeout(function () { el.classList.remove('pop'); }, 110); })(t);
				await delay(90, token);
			}
			await delay(260, token);
			// tiles flip to colors
			for (var c2 = 0; c2 < 5; c2++) {
				if (token !== state.token) return;
				var t2 = tile(rowIdx, c2);
				t2.classList.add('flipping');
				(function (el, color) {
					setTimeout(function () {
						el.classList.add(color);
						el.classList.remove('flipping');
					}, 140);
				})(t2, turn.pattern[c2]);
				await delay(170, token);
			}
			await delay(150, token);
			addTurnRow(turn);
			setCandidates(turn.candidatesAfter);
		}

		async function newGame() {
			var token = ++state.token;
			buildBoard();
			turnsEl.innerHTML = '';
			statusEl.textContent = '';
			statusEl.className = '';
			setCandidates(TOTAL);

			var seedTxt = document.getElementById('seedBox').value.trim();
			var url = 'simulator/game' + (seedTxt ? '?seed=' + encodeURIComponent(seedTxt) : '');
			var game;
			try {
				var res = await fetch(url);
				if (!res.ok) { throw new Error('HTTP ' + res.status); }
				game = await res.json();
			} catch (e) {
				statusEl.textContent = 'failed to fetch game: ' + e.message;
				statusEl.className = 'loss';
				return;
			}
			if (token !== state.token) return;

			for (var i = 0; i < game.turns.length; i++) {
				await playTurn(game.turns[i], i, token);
				if (token !== state.token) return;
				await delay(420, token);
			}

			if (game.solved) {
				statusEl.textContent = 'solved in ' + game.numGuesses;
				statusEl.className = 'win';
			} else {
				statusEl.textContent = 'failed — answer was ' + game.answer.toUpperCase();
				statusEl.className = 'loss';
			}

			if (document.getElementById('loop').checked) {
				await delay(1800, token);
				if (token === state.token) { newGame(); }
			}
		}

		buildBoard();
		newGame();
	})();
	</script>
</body>
</html>
