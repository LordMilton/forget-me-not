package com.example.everydaytodolist

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.everydaytodolist.data.Todo
import com.example.everydaytodolist.data.TodoSorter
import com.example.everydaytodolist.receivers.AlarmReceiver
import com.example.everydaytodolist.ui.screens.EditTaskComposable
import com.example.everydaytodolist.ui.screens.TodoList
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val storageFilename = "storedTodoList"
        // Todos automatically count up unique ids every time they're created so we need to be careful
        // about calling the Todo constructor in navigation composables which are called multiple times for...
        // animations? https://stackoverflow.com/questions/69176617/jetpack-compose-navhost-recomposition-composable-multiple-times
        // Be super careful about doing anything (Read: Don't do anything) with freshTodo besides setting it to an entirely new Todo()
        var freshTodo = Todo()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EverydayToDoListTheme {
                val navController = rememberNavController()

                val context = this

                val sortedBy = TodoSorter.SortMethod.DUE_DATE
                val todoList = remember {
                    {
                        val list = (Todo.readTodosFromFile(
                            File(
                                context.filesDir,
                                storageFilename
                            )) ?: listOf<Todo>())
                            .toMutableStateList()
                        TodoSorter.sort(list, sortedBy)
                        list
                    }()
                }
                val wroteToFile = Todo.writeTodosToFile(todoList, File(context.filesDir, storageFilename))
                if(!wroteToFile) println("Failed to write todos to persistent storage")

                if(ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED)
                {
                    createNotificationChannel(this)
                    val alarmManager = this.getSystemService(ALARM_SERVICE) as AlarmManager
                    val baseTodoIdUri = "content://todos".toUri()
                    for (todo in todoList) {
                        val todoIdUri = Uri.withAppendedPath(baseTodoIdUri, todo.getUniqueId().toString())
                        val notificationIntent = Intent("Todo Notification",
                            todoIdUri,
                            this,
                            AlarmReceiver::class.java)
                        val pendingIntent = PendingIntent.getBroadcast(
                            this,
                            1,
                            notificationIntent,
                            PendingIntent.FLAG_IMMUTABLE)
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, todo.getNextOccurrenceTime().time, pendingIntent)
                    }
                }

                val onNewTodoRequested =
                    {
                        navController.navigate("editor_view")
                        freshTodo = Todo()
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
                            TodoSorter.sort(todoList, sortedBy)
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
                            TodoSorter.sort(todoList, sortedBy)
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
                                        freshTodo,
                                        {
                                            todoList.add(it)
                                            TodoSorter.sort(todoList, sortedBy)
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
                                                TodoSorter.sort(todoList, sortedBy)
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