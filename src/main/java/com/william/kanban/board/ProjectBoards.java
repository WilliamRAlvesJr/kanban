package com.william.kanban.board;

import com.william.kanban.project.ProjectAccess;
import java.util.List;

record ProjectBoards(List<Board> boards, ProjectAccess access) {
}
