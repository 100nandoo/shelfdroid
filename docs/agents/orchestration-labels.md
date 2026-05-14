# Orchestration Labels

This file defines the repo's planning labels for dependency stages, parallel lanes, and chat routing.

Use it when planning issues for multiple agent chats or when `to-issues` needs to assign orchestration labels on publish.

## Label format

Each issue gets exactly one orchestration label.

- Foundation stages: `f<N>-<C>`
- Parallel stages: `p<N><L>-<C>`

Where:

- `N` is the dependency stage number
- `L` is the lane letter within a parallel stage
- `C` is the chat identifier

`N` is the stage number, not a hardcoded mode switch. Any stage can be:

- `f<N>` if it is a single serial stage
- `p<N><L>` if it fans out into parallel lanes

## Rules

- Keep orchestration labels stable after issue creation unless the dependency structure was wrong
- Keep exact blockers in the issue body even when the stage label implies ordering
- Use the same chat id for multiple issues when reducing coordination overhead matters more than parallelism
- Use different chat ids for independent lanes when maximizing parallelism matters more than minimizing chat count

## Example

- `f1-1`
- `p2a-2`
- `p2b-3`
- `f3-4`
