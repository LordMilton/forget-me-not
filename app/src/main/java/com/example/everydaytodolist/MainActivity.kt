package com.example.everydaytodolist

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.notifications.NotificationFactory
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
                if(!wroteToFile) println("Failed to write todos to persistent storage")

                if(ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED)
                {
                    val notificationIntent = Intent()
                    createNotificationChannel(this)
                    val notificationManager = remember { NotificationManagerCompat.from(this) }
                    val notificationFac = remember { NotificationFactory(this) }
                    for ((i,todo) in todoList.withIndex()) {
                        val notification = notificationFac.createTodoDue(todo)
                        notificationManager.notify(i, notification) // TODO Really need to give todos unique ids instead of depending on list ordering
                    }
                }

                val onNewTodoRequested =
                    {
                        navController.navigate("editor_view")
                    }
                val onTodoEditClicked =
                    { todoId: Int ->
                        navController.navigate("editor_view?todoId=${todoId}")
                    }
                val onTodoDeleteClicked: (Int) -> Unit =
                    { todoId: Int ->
                        val (index, todo) = getTodoFromListById(todoList, todoId)
                        if(todo != null) {
                            todoList.removeAt(index)
                        } else {
                            println("Tried to delete a nonexistent todo with id $todoId")
                        }
                    }
                val onTodoCompletedClicked: (Int) -> Unit =
                    { todoId: Int ->
                        var (index, todo) = getTodoFromListById(todoList, todoId)
                        if(todo != null) {
                            todoList.removeAt(index)
                            todo = Todo.copy(todo)
                            todo.markCompleted()
                            todoList.add(index, todo)
                        } else {
                            println("Tried to mark a nonexistent todo as completed with id $todoId")
                        }
                    }
                val onTodoSnoozedClicked: (Int, Int?) -> Unit =
                    { todoId: Int, snoozeLength: Int? ->
                        var (index, todo) = getTodoFromListById(todoList, todoId)
                        if(todo != null){
                            todoList.removeAt(index)
                            todo = Todo.copy(todo)
                            todo.snooze(snoozeLength ?: 1)
                            todoList.add(index, todo)
                        } else {
                            println("Tried to snooze a nonexistent todo with id $todoId")
                        }
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
                                -1 -> {
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
                                }

                                in 0..Int.MAX_VALUE -> {
                                    val (index, referencedTodo) = getTodoFromListById(todoList, todoId)
                                    if(referencedTodo != null) {
                                        EditTaskComposable(
                                            referencedTodo,
                                            {
                                                todoList[index] = it // No need to copy, composition will take place either way since we're switching composables
                                                navController.navigate("list_view")
                                            },
                                            onCancel = {
                                                navController.navigate("list_view")
                                            }
                                        )
                                    }
                                    else {
                                        println("Tried to edit a nonexistent todo with id $todoId")
                                        todoListView() //TODO Indicate to the user that something went wrong
                                    }
                                }

                                else -> todoListView() //TODO Indicate to the user that something went wrong
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is not in the Support Library.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val id = getString(context, R.string.channel_id)
        val name = getString(context, R.string.channel_name)
        val descriptionText = getString(context, R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            getSystemService(context, NotificationManager::class.java) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun getTodoFromListById(todoList: List<Todo>, id: Int): Pair<Int, Todo?> {
    for ((i,todo) in todoList.withIndex()) {
        if (todo.getUniqueId() == id) {
            return Pair(i,todo)
        }
    }
    return Pair(-1, null)
}