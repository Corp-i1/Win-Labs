const { Octokit } = require("@octokit/rest");

const token = process.env.GITHUB_TOKEN;
const [owner, repo] = process.env.REPO.split("/");
const issueNumber = parseInt(process.env.ISSUE_NUMBER);

const octokit = new Octokit({ auth: token });

/* -------------------------
   RELATIONSHIP PARSING
--------------------------*/

// Extract parent from issue body
function extractParent(body) {
  if (!body) return null;
  const match = body.match(/Parent:\s*#(\d+)/i);
  return match ? parseInt(match[1]) : null;
}

// Find children using GitHub search (FAST)
async function findChildren(parentNumber) {
  const query = `repo:${owner}/${repo} "Parent: #${parentNumber}" in:body state:open`;

  const result = await octokit.search.issuesAndPullRequests({
    q: query,
    per_page: 100,
  });

  return result.data.items.map(i => i.number);
}

/* -------------------------
   TREE RESOLUTION
--------------------------*/

// Find root issue
async function findRoot(issueNum) {
  let current = issueNum;
  let visited = new Set();

  let rootTitle = null;

  while (true) {
    if (visited.has(current)) break;
    visited.add(current);

    const issue = await octokit.issues.get({
      owner,
      repo,
      issue_number: current,
    });

    if (!rootTitle) {
      rootTitle = issue.data.title; // capture root title
    }

    const parent = extractParent(issue.data.body);

    if (!parent) break;
    current = parent;
  }

  return {
    root: current,
    title: rootTitle || `Issue #${current}`,
  };
}

// Collect full tree (recursive)
async function collectTree(root) {
  const visited = new Set();

  async function dfs(issueNum) {
    if (visited.has(issueNum)) return;
    visited.add(issueNum);

    const children = await findChildren(issueNum);

    for (const child of children) {
      await dfs(child);
    }
  }

  await dfs(root);
  return [...visited];
}

/* -------------------------
   GIT HELPERS
--------------------------*/

// Get default branch dynamically
async function getDefaultBranch() {
  const repoData = await octokit.repos.get({ owner, repo });
  return repoData.data.default_branch;
}

// Ensure branch exists
async function ensureBranch(branchName) {
  try {
    await octokit.git.getRef({
      owner,
      repo,
      ref: `heads/${branchName}`,
    });
    console.log("Branch exists:", branchName);
    return;
  } catch {}

  const defaultBranch = await getDefaultBranch();

  const baseRef = await octokit.git.getRef({
    owner,
    repo,
    ref: `heads/${defaultBranch}`,
  });

  await octokit.git.createRef({
    owner,
    repo,
    ref: `refs/heads/${branchName}`,
    sha: baseRef.data.object.sha,
  });

  console.log("Created branch:", branchName);
}

/* -------------------------
   Init Commit
--------------------------*/

function encodeBase64(str) {
  return Buffer.from(str).toString("base64");
}

async function createMarkerCommit(branchName, root, issues) {
  const defaultBranch = await getDefaultBranch();

  const content = `
# Issue Group ${root}

## Root
${root}

## Issues
${issues.join(", ")}

## Auto-generated
Created by GitHub Action
`;

  const path = `issue-groups/group-${root}.md`;

  await octokit.repos.createOrUpdateFileContents({
    owner,
    repo,
    path,
    message: `chore: init issue group #${root}`,
    content: encodeBase64(content),
    branch: branchName,
  });

  console.log("Marker commit created");
}

async function hasDiff(branchName, baseBranch) {
  const compare = await octokit.repos.compareCommits({
    owner,
    repo,
    base: baseBranch,
    head: branchName,
  });

  return compare.data.files && compare.data.files.length > 0;
}

/* -------------------------
   PR MANAGEMENT
--------------------------*/

async function ensurePR(branchName, root, rootTitle) {
  const defaultBranch = await getDefaultBranch();

  const prs = await octokit.pulls.list({
    owner,
    repo,
    state: "open",
    head: `${owner}:${branchName}`,
  });

  if (prs.data.length > 0) {
    console.log("PR exists:", prs.data[0].number);
    return prs.data[0];
  }

  const pr = await octokit.pulls.create({
    owner,
    repo,
    title: `${rootTitle} (Group #${root})`,
    head: branchName,
    base: defaultBranch,
    body: `Auto-generated PR for issue group rooted at #${root}\n\nRoot: ${rootTitle}`,
  });

  console.log("Created PR:", pr.data.number);
  return pr.data;
}

/* -------------------------
   ISSUE ANNOTATION
--------------------------*/

async function annotateIssues(issues, branch, pr) {
  for (const num of issues) {
    const issue = await octokit.issues.get({
      owner,
      repo,
      issue_number: num,
    });

    const marker = `<!-- ISSUE_GROUP:${branch} -->`;

    // Idempotency check
    if (issue.data.body.includes(marker)) {
      continue;
    }

    const bodyUpdate = `
${marker}
---
**Branch:** \`${branch}\`
**PR:** #${pr.number}
`;

    await octokit.issues.update({
      owner,
      repo,
      issue_number: num,
      body: issue.data.body + bodyUpdate,
    });

    console.log(`Annotated issue #${num}`);
  }
}

/* -------------------------
   MAIN
--------------------------*/

(async () => {
  try {
    console.log("Processing issue:", issueNumber);

    const { root, title } = await findRoot(issueNumber);
    console.log("Root issue:", root, title);

    const group = await collectTree(root);
    console.log("Group:", group);

    const branchName = `issue-group-${root}`;

    await ensureBranch(branchName);

    const pr = await ensurePR(branchName, root, title);

    await annotateIssues(group, branchName, pr);

    console.log("Done.");
  } catch (err) {
    console.error("Error:", err);
    process.exit(1);
  }
})();