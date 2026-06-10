<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
	<title>Wordle Daily Solver</title>
	<meta charset="UTF-8">
	<style>
		body { background-color: black; color: #d7dadc; font-family: 'Segoe UI', Arial, sans-serif; margin: 0; }
		header { background-color: #1A43BF; padding: 14px 24px; display: flex; align-items: baseline; gap: 16px; }
		header h1 { margin: 0; font-size: 1.4em; color: white; letter-spacing: 2px; }
		header a { color: #c9d4f5; font-size: 0.85em; text-decoration: none; }
		header a:hover { color: white; }
		main { max-width: 920px; margin: 0 auto; padding: 24px; }

		/* tabs */
		#tabs { display: flex; gap: 8px; margin-bottom: 24px; }
		.tab { background: #1a1a1b; border: 1px solid #3a3a3c; color: #9fa3a6; border-radius: 6px 6px 0 0;
		       padding: 10px 22px; cursor: pointer; font-size: 0.95em; }
		.tab.active { background: #1A43BF; border-color: #1A43BF; color: white; }
		section { display: none; }
		section.active { display: block; }

		/* tiles */
		.tilerow { display: flex; gap: 6px; }
		.tile { width: 56px; height: 56px; border: 2px solid #3a3a3c; box-sizing: border-box;
		        display: flex; align-items: center; justify-content: center;
		        font-size: 1.8em; font-weight: bold; text-transform: uppercase; color: #d7dadc; }
		.tile.G { background-color: #538d4e; border-color: #538d4e; }
		.tile.Y { background-color: #b59f3b; border-color: #b59f3b; }
		.tile.B { background-color: #3a3a3c; border-color: #3a3a3c; }
		.tile.suggest { border-color: #1A43BF; }
		.tile.clickable { cursor: pointer; }
		.tile.flipping { transform: rotateX(90deg); transition: transform 0.14s ease-in; }
		.tile.pop { transform: scale(1.12); border-color: #565758; }

		/* assist layout */
		.label { font-size: 0.78em; color: #818384; text-transform: uppercase; letter-spacing: 1px;
		         margin: 18px 0 8px 0; }
		#assistMsg { min-height: 1.3em; margin-top: 12px; font-size: 0.9em; }
		#assistMsg.err { color: #cc4444; }
		#assistMsg.win { color: #538d4e; font-size: 1.1em; }
		.hint { font-size: 0.78em; color: #565758; margin-top: 6px; }
		.statline { margin-top: 20px; font-size: 0.95em; color: #9fa3a6; }
		.statline b { color: white; font-size: 1.3em; }
		#candBarWrap, #candBarWrap2 { background: #1a1a1b; border: 1px solid #3a3a3c; height: 12px;
		               border-radius: 6px; overflow: hidden; margin: 8px 0 4px 0; max-width: 460px; }
		#candBar, #candBar2 { background: linear-gradient(90deg, #1A43BF, #538d4e); height: 100%; width: 100%;
		           transition: width 0.5s ease; }
		.sample { font-size: 0.8em; color: #565758; }
		.sample span { color: #818384; }

		.turnRow { border: 1px solid #2a2a2c; border-radius: 6px; padding: 8px 12px; margin: 8px 0;
		           display: flex; align-items: center; gap: 12px; max-width: 460px; }
		.turnWord { font-weight: bold; text-transform: uppercase; letter-spacing: 2px; width: 86px; }
		.mini { display: inline-block; width: 11px; height: 11px; margin-right: 2px; border-radius: 2px; }
		.mini.G { background: #538d4e; } .mini.Y { background: #b59f3b; } .mini.B { background: #3a3a3c; }
		.bits { margin-left: auto; font-size: 0.8em; color: #9fa3a6; text-align: right; }
		.bits b { color: #d7dadc; }

		button { background: #1A43BF; color: white; border: none; border-radius: 5px;
		         padding: 9px 16px; font-size: 0.95em; cursor: pointer; }
		button:hover { background: #2a55d8; }
		button.secondary { background: #3a3a3c; }
		button.secondary:hover { background: #4a4a4c; }
		button:disabled { background: #2a2a2c; color: #565758; cursor: default; }
		input[type=text] { background: #1a1a1b; border: 1px solid #3a3a3c; color: #d7dadc;
		                   border-radius: 4px; padding: 8px 10px; width: 120px; font-size: 1em;
		                   text-transform: uppercase; letter-spacing: 2px; }
		.btnrow { display: flex; gap: 10px; margin-top: 16px; align-items: center; flex-wrap: wrap; }

		/* auto */
		#autoGate { text-align: center; padding: 40px 0; }
		#autoGate p { color: #818384; font-size: 0.9em; }
		#autoArea { display: none; }
		#autoMeta { color: #9fa3a6; font-size: 0.9em; margin-bottom: 16px; }
		#autoBoard { display: grid; grid-template-rows: repeat(6, 62px); gap: 6px; width: fit-content; }
		#autoBoard .row { display: grid; grid-template-columns: repeat(5, 62px); gap: 6px; }
		#autoBoard .tile { width: 62px; height: 62px; font-size: 2em; }
		#autoStatus { margin-top: 14px; min-height: 1.4em; font-size: 1.05em; }
		#autoStatus.win { color: #538d4e; }
		#autoStatus.loss { color: #cc4444; }
		#autoWrap { display: flex; gap: 36px; flex-wrap: wrap; }
		#autoTurns { max-width: 460px; }
	</style>
</head>
<body id="dailyPage">
	<header>
		<h1>DAILY NYT</h1>
		<a href=".">&larr; home</a>
	</header>
	<main>
		<div id="tabs">
			<div class="tab active" id="tabAssist">Assist (no peeking)</div>
			<div class="tab" id="tabAuto">Auto-solve (spoilers!)</div>
		</div>

		<!-- ============================ ASSIST ============================ -->
		<section id="assistSection" class="active">
			<div class="label">engine suggests</div>
			<div class="tilerow" id="suggestTiles"></div>
			<div class="hint" id="suggestBits"></div>

			<div class="label">what did you play? click tiles to set the colors NYT showed you</div>
			<div class="btnrow">
				<input type="text" id="guessBox" maxlength="5" spellcheck="false">
			</div>
			<div class="tilerow" id="feedbackTiles" style="margin-top:10px"></div>
			<div class="hint">tap a tile to cycle gray &rarr; yellow &rarr; green</div>

			<div class="btnrow">
				<button id="applyBtn">Apply feedback</button>
				<button id="undoBtn" class="secondary">Undo</button>
				<button id="restartBtn" class="secondary">Restart</button>
			</div>
			<div id="assistMsg"></div>

			<div class="statline"><b id="candCount">2,309</b> candidates remaining</div>
			<div id="candBarWrap"><div id="candBar"></div></div>
			<div class="sample" id="candSample"></div>
			<div id="assistTurns"></div>
		</section>

		<!-- ============================= AUTO ============================= -->
		<section id="autoSection">
			<div id="autoGate">
				<button id="autoGo">Fetch &amp; solve today's puzzle</button>
				<p>This downloads today's solution from the NYT and reveals it. No way back!</p>
			</div>
			<div id="autoArea">
				<div id="autoMeta"></div>
				<div id="autoWrap">
					<div>
						<div id="autoBoard"></div>
						<div id="autoStatus"></div>
					</div>
					<div id="autoTurns">
						<div class="statline"><b id="candCount2">2,309</b> candidates remaining</div>
						<div id="candBarWrap2"><div id="candBar2"></div></div>
					</div>
				</div>
			</div>
		</section>
	</main>

	<script>
	(function () {
		var TOTAL = 2309;
		var LOG_TOTAL = Math.log(TOTAL + 1);

		// ------------------------------------------------------------ tabs
		var tabAssist = document.getElementById('tabAssist');
		var tabAuto = document.getElementById('tabAuto');
		function showTab(which) {
			tabAssist.classList.toggle('active', which === 'assist');
			tabAuto.classList.toggle('active', which === 'auto');
			document.getElementById('assistSection').classList.toggle('active', which === 'assist');
			document.getElementById('autoSection').classList.toggle('active', which === 'auto');
		}
		tabAssist.addEventListener('click', function () { showTab('assist'); });
		tabAuto.addEventListener('click', function () { showTab('auto'); });

		function post(url, body) {
			return fetch(url, {
				method: 'POST',
				headers: { 'Content-Type': 'application/json' },
				body: body ? JSON.stringify(body) : '{}'
			}).then(function (r) {
				if (!r.ok) { throw new Error('HTTP ' + r.status); }
				return r.json();
			});
		}

		function miniTiles(pattern) {
			var html = '';
			for (var i = 0; i < 5; i++) { html += '<span class="mini ' + pattern[i] + '"></span>'; }
			return html;
		}

		function turnRowHtml(turn) {
			return '<div class="turnRow"><span class="turnWord">' + turn.word + '</span>'
				+ '<span>' + miniTiles(turn.pattern) + '</span>'
				+ '<span class="bits">expected <b>' + turn.expectedBits.toFixed(2) + '</b> bits<br>'
				+ 'gained <b>' + turn.bitsGained.toFixed(2) + '</b> &rarr; <b>'
				+ turn.candidatesAfter.toLocaleString() + '</b> left</span></div>';
		}

		// ---------------------------------------------------------- assist
		var guessBox = document.getElementById('guessBox');
		var suggestTiles = document.getElementById('suggestTiles');
		var feedbackTiles = document.getElementById('feedbackTiles');
		var assistMsg = document.getElementById('assistMsg');
		var colors = [0, 0, 0, 0, 0]; // 0=B 1=Y 2=G
		var COLOR_CHARS = ['B', 'Y', 'G'];
		var gameOver = false;

		function renderSuggestion(state) {
			var word = state.suggestion || '';
			var html = '';
			for (var i = 0; i < 5; i++) {
				html += '<div class="tile suggest">' + (word[i] || '') + '</div>';
			}
			suggestTiles.innerHTML = html;
			document.getElementById('suggestBits').textContent = state.expectedBits != null
				? 'expected information: ' + state.expectedBits.toFixed(2) + ' bits'
				: '';
		}

		function renderFeedbackTiles() {
			var word = guessBox.value.toLowerCase();
			var html = '';
			for (var i = 0; i < 5; i++) {
				html += '<div class="tile clickable ' + COLOR_CHARS[colors[i]] + '" data-i="' + i + '">'
					+ (word[i] || '') + '</div>';
			}
			feedbackTiles.innerHTML = html;
			var tiles = feedbackTiles.children;
			for (var j = 0; j < tiles.length; j++) {
				tiles[j].addEventListener('click', function () {
					if (gameOver) { return; }
					var i = parseInt(this.getAttribute('data-i'), 10);
					colors[i] = (colors[i] + 1) % 3;
					renderFeedbackTiles();
				});
			}
		}
		guessBox.addEventListener('input', renderFeedbackTiles);

		function setAssistCandidates(state) {
			document.getElementById('candCount').textContent = state.candidates.toLocaleString();
			var pct = Math.max(2, 100 * Math.log(state.candidates + 1) / LOG_TOTAL);
			document.getElementById('candBar').style.width = pct + '%';
			var sampleEl = document.getElementById('candSample');
			if (state.candidates > 0 && state.candidates <= 50) {
				var s = state.remainingSample.join(', ');
				if (state.candidates > state.remainingSample.length) { s += ', …'; }
				sampleEl.innerHTML = 'could be: <span>' + s + '</span>';
			} else {
				sampleEl.textContent = '';
			}
		}

		function applyState(state) {
			renderSuggestion(state);
			setAssistCandidates(state);

			var turnsEl = document.getElementById('assistTurns');
			var html = '';
			for (var i = 0; i < state.turns.length; i++) { html += turnRowHtml(state.turns[i]); }
			turnsEl.innerHTML = html;

			gameOver = state.solved || state.failed;
			assistMsg.className = '';
			if (state.error) {
				assistMsg.className = 'err';
				assistMsg.textContent = state.error;
			} else if (state.solved) {
				assistMsg.className = 'win';
				assistMsg.textContent = 'Solved in ' + state.turns.length + '!';
			} else if (state.failed) {
				assistMsg.className = 'err';
				assistMsg.textContent = 'Out of guesses — better luck tomorrow.';
			} else if (state.inconsistent) {
				assistMsg.className = 'err';
				assistMsg.textContent = 'No word matches that feedback — a pattern was probably entered wrong. Use Undo.';
			} else {
				assistMsg.textContent = '';
			}

			if (!gameOver && state.suggestion) {
				guessBox.value = state.suggestion.toUpperCase();
			}
			colors = [0, 0, 0, 0, 0];
			renderFeedbackTiles();
			document.getElementById('applyBtn').disabled = gameOver || state.inconsistent;
		}

		document.getElementById('applyBtn').addEventListener('click', function () {
			var pattern = '';
			for (var i = 0; i < 5; i++) { pattern += COLOR_CHARS[colors[i]]; }
			post('daily/assist/feedback', { guess: guessBox.value, pattern: pattern })
				.then(applyState)
				.catch(function (e) { assistMsg.className = 'err'; assistMsg.textContent = e.message; });
		});
		document.getElementById('undoBtn').addEventListener('click', function () {
			post('daily/assist/undo').then(applyState);
		});
		document.getElementById('restartBtn').addEventListener('click', function () {
			post('daily/assist/start').then(applyState);
		});

		// ------------------------------------------------------------ auto
		var autoBoard = document.getElementById('autoBoard');

		function buildAutoBoard() {
			autoBoard.innerHTML = '';
			for (var r = 0; r < 6; r++) {
				var row = document.createElement('div');
				row.className = 'row';
				for (var c = 0; c < 5; c++) {
					var t = document.createElement('div');
					t.className = 'tile';
					row.appendChild(t);
				}
				autoBoard.appendChild(row);
			}
		}
		function autoTile(r, c) { return autoBoard.children[r].children[c]; }
		function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }

		function setAutoCandidates(n) {
			document.getElementById('candCount2').textContent = n.toLocaleString();
			var pct = Math.max(2, 100 * Math.log(n + 1) / LOG_TOTAL);
			document.getElementById('candBar2').style.width = pct + '%';
		}

		async function playAutoTurn(turn, rowIdx) {
			for (var c = 0; c < 5; c++) {
				var t = autoTile(rowIdx, c);
				t.textContent = turn.word[c];
				t.classList.add('pop');
				(function (el) { setTimeout(function () { el.classList.remove('pop'); }, 110); })(t);
				await sleep(90);
			}
			await sleep(260);
			for (var c2 = 0; c2 < 5; c2++) {
				var t2 = autoTile(rowIdx, c2);
				t2.classList.add('flipping');
				(function (el, color) {
					setTimeout(function () {
						el.classList.add(color);
						el.classList.remove('flipping');
					}, 140);
				})(t2, turn.pattern[c2]);
				await sleep(170);
			}
			await sleep(150);
			document.getElementById('autoTurns').insertAdjacentHTML('beforeend', turnRowHtml(turn));
			setAutoCandidates(turn.candidatesAfter);
		}

		document.getElementById('autoGo').addEventListener('click', async function () {
			this.disabled = true;
			var game;
			try {
				var res = await fetch('daily/auto/game');
				if (!res.ok) { throw new Error('HTTP ' + res.status); }
				game = await res.json();
				if (game.error) { throw new Error(game.error); }
			} catch (e) {
				document.getElementById('autoGate').querySelector('p').textContent = e.message;
				this.disabled = false;
				return;
			}
			document.getElementById('autoGate').style.display = 'none';
			document.getElementById('autoArea').style.display = 'block';
			document.getElementById('autoMeta').textContent =
				'Wordle #' + game.puzzleId + ' — ' + game.printDate
				+ (game.editor ? ' — edited by ' + game.editor : '');
			buildAutoBoard();
			setAutoCandidates(TOTAL);
			for (var i = 0; i < game.turns.length; i++) {
				await playAutoTurn(game.turns[i], i);
				await sleep(420);
			}
			var st = document.getElementById('autoStatus');
			if (game.solved) {
				st.textContent = 'solved in ' + game.numGuesses;
				st.className = 'win';
			} else {
				st.textContent = 'failed — answer was ' + game.answer.toUpperCase();
				st.className = 'loss';
			}
		});

		// boot: start a fresh assist game
		post('daily/assist/start').then(applyState);
	})();
	</script>
</body>
</html>
