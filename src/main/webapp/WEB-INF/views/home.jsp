<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html>
<head>
	<title>Wordle Solver</title>
	<meta charset="UTF-8">
	<style>
		body { background-color: black; color: #d7dadc; font-family: 'Segoe UI', Arial, sans-serif; margin: 0; }
		header { background-color: #1A43BF; padding: 16px 24px; }
		header h1 { margin: 0; font-size: 1.6em; color: white; letter-spacing: 2px; }
		main { padding: 24px; }
		.mode { display: inline-block; margin: 12px 12px 0 0; padding: 16px 20px; border: 1px solid #3a3a3c;
		        border-radius: 8px; color: #d7dadc; text-decoration: none; min-width: 200px; }
		.mode:hover { border-color: #538d4e; }
		.mode h2 { margin: 0 0 6px 0; font-size: 1.1em; color: #538d4e; }
		.mode p { margin: 0; font-size: 0.85em; color: #818384; }
		.stats { margin-top: 28px; font-size: 0.8em; color: #565758; }
	</style>
</head>
<body id="homePage">
	<header><h1>WORDLE — SELF-PLAYING</h1></header>
	<main>
		<a class="mode" href="simulator">
			<h2>Simulator</h2>
			<p>Watch the engine solve random answers</p>
		</a>
		<a class="mode" href="daily">
			<h2>Daily NYT</h2>
			<p>Auto-solve or assist today's puzzle</p>
		</a>
		<a class="mode" href="benchmark">
			<h2>Benchmark</h2>
			<p>Compare openers and strategies</p>
		</a>
		<div class="stats">
			engine: <c:out value="${answerCount}"/> answers ·
			<c:out value="${guessCount}"/> valid guesses ·
			opener <c:out value="${opener}"/>
		</div>
	</main>
</body>
</html>
