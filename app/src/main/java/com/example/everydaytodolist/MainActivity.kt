package com.example.everydaytodolist

import android.os.Bundle
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.ui.screens.EditTaskComposable
import com.example.everydaytodolist.ui.screens.TodoList
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EverydayToDoListTheme {
                val navController = rememberNavController()

                val todoList = remember { mutableListOf<Todo>() }
                val onNewTodoRequested = {
                    navController.navigate("editor_view")
                }
                val onTodoEditClicked = { todoId: Int ->
                    navController.navigate("editorView?${todoId}")
                }
                val onTodoDeleteClicked: (Int) -> Unit = { todoId: Int ->
                    todoList.removeAt(todoId)
                }
                val todoListView = TodoList(
                    todoList,
                    onNewTodoRequested,
                    onTodoEditClicked,
                    onTodoDeleteClicked
                )

                NavHost(
                    navController = navController,
                    startDestination = "list_view",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(route = "list_view") {
                        todoListView
                    }
                    composable(
                        route = "editor_view?todoItem",
                        arguments = listOf(
                            navArgument("todoItem") {
                                NavType.IntType
                                nullable = true
                            }
                        )
                    ) { curBackStackEntry ->
                        val arguments = curBackStackEntry.arguments
                        val todoId = arguments?.getInt("todoItem")
                        when(todoId) {
                            null ->
                                EditTaskComposable(
                                Todo(),
                                {
                                        todoList.add(it)
                                    }
                                )
                            in 0..(todoList.size-1) ->
                                EditTaskComposable(
                                    todoList[todoId],
                                    {
                                        todoList[todoId] = it
                                    }
                                )
                            else -> todoListView //TODO Indicate to the user that something went wrong
                        }
                    }
                }
            }
        }
    }
}