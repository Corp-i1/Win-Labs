const { Octokit } = require("@octokit/rest");

const token = process.env.GITHUB_TOKEN;
const [owner, repo] = process.env.REPO.split("/");
const issueNumber = parseInt(process.env.ISSUE_NUMBER);

const octokit = new Octokit({ auth: token });

// Extract child issue numbers from body
function extractChildren(body) {
  const regex = /#(\d+)/g;
  const matches = [...body.matchAll(regex)];
  return matches.map(m => parseInt(m[1]));
}

// Find parent issue (reverse lookup)
async function findParent(issueNum) {
  const issues = await octokit.paginate(octokit.issues.listForRepo, {
    owner,
    repo,
    state: "open",
  });

  for (const issue of issues) {
    const children = extractChildren(issue.body || "");
    if (children.includes(issueNum)) {
      return issue.number;
    }
  }
  return null;
}

// Recursively find root parent
async function findRoot(issueNum) {
  let current = issueNum;
  let parent;

  while ((parent = await findParent(current))) {
    current = parent;
  }

  return current;
}

// Recursively collect all descendants
async function collectTree(root) {
  const visited = new Set();

  async function dfs(issueNum) {
    if (visited.has(issueNum)) return;
    visited.add(issueNum);

    const issue = await octokit.issues.get({
      owner,
      repo,
      issue_number: issueNum,
    });

    const children = extractChildren(issue.data.body || "");
    for (const child of children) {
      await dfs(child);
    }
  }

  await dfs(root);
  return [...visited];
}

// Ensure branch exists
async function ensureBranch(branchName) {
  try {
    await octokit.git.getRef({
      owner,
      repo,
      ref: `heads/${branchName}`,
    });
    return;
  } catch {}

  const main = await octokit.git.getRef({
    owner,
    repo,
    ref: "heads/main",
  });

  await octokit.git.createRef({
    owner,
    repo,
    ref: `refs/heads/${branchName}`,
    sha: main.data.object.sha,
  });
}

// Ensure PR exists
async function ensurePR(branchName, root) {
  const prs = await octokit.pulls.list({
    owner,
    repo,
    state: "open",
    head: `${owner}:${branchName}`,
  });

  if (prs.data.length > 0) {
    return prs.data[0];
  }

  const pr = await octokit.pulls.create({
    owner,
    repo,
    title: `Group for #${root}`,
    head: branchName,
    base: "main",
    body: `Auto-generated PR for issue group rooted at #${root}`,
  });

  return pr.data;
}

// Assign branch/PR to issues
async function annotateIssues(issues, branch, pr) {
  for (const num of issues) {
    const bodyUpdate = `\n\n---\n**Branch:** \`${branch}\`\n**PR:** #${pr.number}\n`;

    
    const issue = await octokit.issues.get({
      owner,
      repo,
      issue_number: num,
    });

    if (!issue.data.body.includes(branch)) {
      await octokit.issues.update({
        owner,
        repo,
        issue_number: num,
        body: issue.data.body + bodyUpdate,
      });
    }
  }
}

// MAIN FLOW
(async () => {
  console.log("Processing issue:", issueNumber);

  const root = await findRoot(issueNumber);
  console.log("Root issue:", root);

  const group = await collectTree(root);
  console.log("Group:", group);

  const branchName = `issue-group-${root}`;

  await ensureBranch(branchName);
  const pr = await ensurePR(branchName, root);

  await annotateIssues(group, branchName, pr);

  console.log("Done.");
})();