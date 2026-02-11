package com.example.everydaytodolist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getString
import androidx.core.content.ContextCompat.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.everydaytodolist.alarms.MidnightAlarm
import com.example.everydaytodolist.data.TodoListUtil
import com.example.everydaytodolist.data.TodoSorter
import com.example.everydaytodolist.data.todos.ITodo
import com.example.everydaytodolist.ui.screens.EditTaskComposable
import com.example.everydaytodolist.ui.screens.ListView
import com.example.everydaytodolist.ui.theme.EverydayToDoListTheme
import java.io.File

val Context.preferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {

    val context = this

    suspend fun finishedFirstRunAfterBoot() {
        this.preferencesDataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[booleanPreferencesKey("first_run_after_boot")] = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val storageFilename = this.resources.getString(R.string.todo_storage_file)

        val initiatingIntent = getIntent()
        val focusedTodoId = initiatingIntent.data?.lastPathSegment?.toIntOrNull() ?: -1
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EverydayToDoListTheme {
                val navController = rememberNavController()

                var sortedBy by remember { mutableStateOf(TodoSorter.SortMethod.DUE_DATE) }
                val todoList = remember {
                    {
                        val list = (TodoListUtil.readTodosFromFile(
                            File(
                                context.filesDir,
                                storageFilename
                            )) ?: listOf<ITodo>())
                            .toMutableStateList()
                        TodoSorter.sort(list, sortedBy)
                        list
                    }()
                }
                val wroteToFile = TodoListUtil.writeTodosToFile(todoList, File(context.filesDir, storageFilename))
                if(!wroteToFile) println("Failed to write todos to persistent storage")

                // Check if notifications are enabled (don't remember so this is updated if setting is changed)
                var areNotificationsEnabled by remember {
                    mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
                }
                // Add a lifecycle observer to update the notification status on resume.
                val lifecycleOwner = rememberUpdatedState(newValue = this as LifecycleOwner)
                DisposableEffect(Unit) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                        }
                    }
                    val lifecycle = lifecycleOwner.value.lifecycle
                    lifecycle.addObserver(observer)

                    onDispose {
                        lifecycle.removeObserver(observer)
                    }
                }

                // Set up incomplete todos to rollover at midnight every night
                // (Key is there to avoid recomposition)
                LaunchedEffect(1) { MidnightAlarm.createMidnightAlarms(context) }
                LaunchedEffect(1) { finishedFirstRunAfterBoot() }

                // No longer setting up notifications when app starts, instead happens when the todoObject is first created/edited, during midnight 'alarm', on reboot

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
                            todo = todo.clone() as ITodo
                            val repeat = todo.markCompleted()
                            if(repeat) {
                                todoList.add(index, todo)
                            }
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
                            todo = todo.clone() as ITodo
                            todo.snooze(snoozeLength ?: 1)
                            todoList.add(index, todo)
                            TodoSorter.sort(todoList, sortedBy)
                        } else {
                            println("Tried to snooze a nonexistent todo with id $todoId")
                        }
                    }
                val onSortClicked: (TodoSorter.SortMethod, Boolean) -> Unit =
                    {   sortMethod: TodoSorter.SortMethod, reversed: Boolean ->
                        TodoSorter.sort(todoList, sortMethod, reversed)
                        sortedBy = sortMethod
                    }
                val todoListView: @Composable (focusedTodoId: Int) -> Unit = { ListView(
                    todoList,
                    focusedTodoId,
                    sortedBy,
                    onNewTodoRequested,
                    onTodoEditClicked,
                    onTodoDeleteClicked,
                    onTodoCompletedClicked,
                    onTodoSnoozedClicked,
                    onSortClicked,
                    Modifier
                ) }

                Scaffold(
                    topBar = {
                        // Only show the redirect reminder if notifications are disabled
                        if (!areNotificationsEnabled) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(16.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Enable notifications to receive task reminders",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Button(onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }) {
                                    Text("Go to Settings")
                                }
                            }
                        }
                    },
                    modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "list_view",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(
                            route = "list_view?todoId={todoId}",
                            arguments = listOf(
                                navArgument("todoId") {
                                    NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { curBackStackEntry ->
                            val arguments = curBackStackEntry.arguments
                            val todoId = arguments?.getInt("todoId") ?: -1
                            todoListView(todoId)
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
                                        null,
                                        {
                                            todoList.add(it)
                                            TodoSorter.sort(todoList, sortedBy)
                                            TodoListUtil.createNotificationAlarms(context, listOf(it))
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
                                                TodoListUtil.createNotificationAlarms(context, listOf(it))
                                                navController.navigate("list_view")
                                            },
                                            onCancel = {
                                                navController.navigate("list_view")
                                            }
                                        )
                                    }
                                    else {
                                        println("Tried to edit a nonexistent todo with id $todoId")
                                        todoListView(-1) //TODO Indicate to the user that something went wrong
                                    }
                                }

                                else -> todoListView(-1) //TODO Indicate to the user that something went wrong
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel
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

fun getTodoFromListById(todoList: List<ITodo>, id: Int): Pair<Int, ITodo?> {
    for ((i,todo) in todoList.withIndex()) {
        if (todo.uniqueId == id) {
            return Pair(i,todo)
        }
    }
    return Pair(-1, null)
}