---
name: verify-grade
description: project evaluation to achieve the best possible grade
user-invocable: true
allowed-tools: Bash, Read, Grep
---

# Grade verifier

This project is part of a lecture, lecture software engineering tools and ai

## Context

The lecture markdown and html can be found one in ~/source/lecture-se-tools-ai

## Instructions

### Step 1: Read the Lecture Documents

First of all gain a the full picture on the lecture scope, the entire lecture however doesn't define all grading steps as some parts got cut short.

- **Git**: Git commands, best practices
- **SE-Tools**: maven usage, plugins, profiles etc. Project Structure as required
- **Tests**: Mockito is optional, JUnit has to be the configured and contain Tests with either the AAA-Pattern or given/when/then
- **CI/CD**: Is in fact optional, and if either GitHub CI/CD or Jenkins
- **Project**: Most likely defined as "Student Grade Calculator", unique processes are allowed to implement.

Most important part are the last chapters with the "Grade Loss" qualifications, these define the actual Grade in the End

## Step 2: Evaluate the Project

Gather a decent overview over:

- **Git**: How does the worktree look? Are any IDE configs or secrets pushed/tracked?
- **SE-Tools**: pmd, checkstyle, spotbugs all configured and properly ruled?
- **Tests**: Useful, properly written tests, that follow lecture scope
- **Project**: Properly structured, e.g. paths of packages
- **Docs**: What is documented? Is the architecture thought through?

## Step 3: Compare and Express

Create a table containing all Lecture Grading policies (Column 1), list them and provide the penalty in Column 2,
in Column 3 evaluate as a senior professional wether your standards are met.

Follow up with an explanation for each point, why and if a penalty could be applied.