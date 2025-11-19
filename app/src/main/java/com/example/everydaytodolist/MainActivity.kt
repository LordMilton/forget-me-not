package com.example.everydaytodolist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.ui.screens.EditTaskComposable
import com.example.everydaytodolist.ui.screens.TodoList
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val storageFilename = "storedTodoList"

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EverydayToDoListTheme {
                val navController = rememberNavController()

                val context = this

                val todoList = remember {
                    (Todo.readTodosFromFile(File(context.filesDir, storageFilename)) ?: listOf<Todo>()).toMutableStateList()
                }
                val wroteToFile = Todo.writeTodosToFile(todoList, File(context.filesDir, storageFilename))
                println("Wrote to file: ${if(wroteToFile) "success" else "failure"}")

                val onNewTodoRequested = {
                    navController.navigate("editor_view")
                }
                val onTodoEditClicked = { todoId: Int ->
                    navController.navigate("editor_view?todoId=${todoId}")
                }
                val onTodoDeleteClicked: (Int) -> Unit = { todoId: Int ->
                    todoList.removeAt(todoId)
                }
                val onTodoCompletedClicked: (Int) -> Unit =
                    { todoId: Int ->
                        var todo = todoList.removeAt(todoId)
                        todo = Todo.copy(todo)
                        todo.markCompleted()
                        todoList.add(todoId, todo)
                    }
                val onTodoSnoozedClicked: (Int, Int?) -> Unit =
                    { todoId: Int, snoozeLength: Int? ->
                        var todo = todoList.removeAt(todoId)
                        todo = Todo.copy(todo)
                        todo.snooze(snoozeLength ?: 1)
                        todoList.add(todoId, todo)
                    }
                val todoListView: @Composable () -> Unit = { TodoList(
                    todoList,
                    onNewTodoRequested,
                    onTodoEditClicked,
                    onTodoDeleteClicked,
                    onTodoCompletedClicked,
                    onTodoSnoozedClicked
                ) }

                Scaffold(
                    Modifier
                        .background(color = MaterialTheme.colorScheme.background)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "list_view",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(route = "list_view") {
                            todoListView()
                        }
                        composable(
                            route = "editor_view?todoId={todoId}",
                            arguments = listOf(
                                navArgument("todoId") {
                                    NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { curBackStackEntry ->
                            val arguments = curBackStackEntry.arguments
                            val todoId = arguments?.getInt("todoId") ?: -2
                            when (todoId) {
                                -1 ->
                                    EditTaskComposable(
                                        Todo(),
                                        {
                                            todoList.add(it)
                                            navController.navigate("list_view")
                                        },
                                        onCancel = {
                                            navController.navigate("list_view")
                                        }
                                    )

                                in 0..(todoList.size - 1) ->
                                    EditTaskComposable(
                                        todoList[todoId],
                                        {
                                            todoList[todoId] = it
                                            navController.navigate("list_view")
                                        },
                                        onCancel = {
                                            navController.navigate("list_view")
                                        }
                                    )

                                else -> todoListView() //TODO Indicate to the user that something went wrong
                            }
                        }
                    }
                }
            }
        }
    }
}