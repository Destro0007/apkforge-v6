
package com.apkforge.v6

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val ForgeBackground = Color(0xFF0B1020)
private val ForgePanel = Color(0xFF151D32)
private val ForgeAccent = Color(0xFF65D6C4)
private val ForgeMuted = Color(0xFF9AA8C2)

private val PackagePattern =
    Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")

private enum class ForgeTab(val title: String, val symbol: String) {
    HOME("Home", "⌂"),
    PROJECTS("Projects", "▦"),
    AI("AI Studio", "✦"),
    BUILDS("Builds", "↗"),
    SETTINGS("Settings", "⚙")
}

private data class ForgeProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packageName: String
)

private class ProjectStore(context: Context) {
    private val preferences =
        context.getSharedPreferences("apkforge_workspace", Context.MODE_PRIVATE)

    fun load(): List<ForgeProject> {
        val raw = preferences.getString("projects_json", null)
            ?: return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val name = item.optString("name").trim()
                    val packageName = item.optString("packageName").trim()

                    if (name.isNotEmpty() &&
                        PackagePattern.matches(packageName)
                    ) {
                        add(
                            ForgeProject(
                                id = item.optString("id").ifBlank {
                                    UUID.randomUUID().toString()
                                },
                                name = name,
                                packageName = packageName
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(projects: List<ForgeProject>) {
        val array = JSONArray()

        projects.forEach { project ->
            array.put(
                JSONObject()
                    .put("id", project.id)
                    .put("name", project.name)
                    .put("packageName", project.packageName)
            )
        }

        preferences.edit()
            .putString("projects_json", array.toString())
            .apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = ForgeAccent,
                    background = ForgeBackground,
                    surface = ForgePanel,
                    onPrimary = Color(0xFF07151A),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ForgeBackground
                ) {
                    ApkForgeApp()
                }
            }
        }
    }
}

@Composable
private fun ApkForgeApp() {
    val context = LocalContext.current
    val store = remember(context) { ProjectStore(context) }

    val projects = remember(store) {
        mutableStateListOf<ForgeProject>().apply {
            addAll(store.load())
        }
    }

    var selectedTab by remember { mutableStateOf(ForgeTab.HOME) }
    var openedProjectId by remember { mutableStateOf<String?>(null) }

    fun saveProjects() {
        store.save(projects.toList())
    }

    Scaffold(
        containerColor = ForgeBackground,
        bottomBar = {
            NavigationBar(containerColor = ForgePanel) {
                ForgeTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            if (tab != ForgeTab.PROJECTS) {
                                openedProjectId = null
                            }
                        },
                        icon = {
                            Text(tab.symbol, fontSize = 19.sp)
                        },
                        label = {
                            Text(tab.title, fontSize = 10.sp)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 18.dp)
        ) {
            when (selectedTab) {
                ForgeTab.HOME -> HomeScreen(
                    projectCount = projects.size,
                    onCreateProject = {
                        selectedTab = ForgeTab.PROJECTS
                        openedProjectId = null
                    },
                    onOpenAi = {
                        selectedTab = ForgeTab.AI
                    }
                )

                ForgeTab.PROJECTS -> {
                    val openedProject =
                        projects.firstOrNull { it.id == openedProjectId }

                    if (openedProject == null) {
                        ProjectsScreen(
                            projects = projects,
                            onOpen = { openedProjectId = it.id },
                            onSave = { saveProjects() }
                        )
                    } else {
                        ProjectDetailScreen(
                            project = openedProject,
                            existingProjects = projects.toList(),
                            onBack = {
                                openedProjectId = null
                            },
                            onUpdate = { updated ->
                                val index =
                                    projects.indexOfFirst {
                                        it.id == updated.id
                                    }

                                if (index >= 0) {
                                    projects[index] = updated
                                }

                                saveProjects()
                            },
                            onDelete = {
                                projects.removeAll {
                                    it.id == openedProject.id
                                }

                                saveProjects()
                                openedProjectId = null
                            }
                        )
                    }
                }

                ForgeTab.AI -> InfoScreen(
                    "AI Studio",
                    "Your free-first development workspace.",
                    listOf(
                        "Model routing: planned",
                        "Local model support: planned",
                        "Prompt-to-app generation: planned",
                        "Code explanation and repair: planned"
                    )
                )

                ForgeTab.BUILDS -> InfoScreen(
                    "Cloud Builds",
                    "Build APKs through GitHub Actions.",
                    listOf(
                        "Debug APK workflow: configured",
                        "Build history: planned",
                        "Artifact download: planned",
                        "APK verification: planned"
                    )
                )

                ForgeTab.SETTINGS -> InfoScreen(
                    "Settings",
                    "APKForge keeps the first release free-first.",
                    listOf(
                        "AI provider keys: not configured",
                        "GitHub connection: not connected in app",
                        "Offline workspace: stored on this device",
                        "Automatic repair: planned"
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    projectCount: Int,
    onCreateProject: () -> Unit,
    onOpenAi: () -> Unit
) {
    Spacer(Modifier.height(22.dp))

    Text(
        "APKFORGE",
        color = ForgeAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        "Build something\nremarkable.",
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 36.sp
    )

    Spacer(Modifier.height(8.dp))
    Text(
        "Your AI-powered app factory.",
        color = ForgeMuted,
        fontSize = 14.sp
    )

    Spacer(Modifier.height(22.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricCard("PROJECTS", projectCount.toString(), Modifier.weight(1f))
        MetricCard("BUILDS", "0", Modifier.weight(1f))
        MetricCard("STATUS", "Ready", Modifier.weight(1f))
    }

    Spacer(Modifier.height(20.dp))
    Text("Quick actions", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))

    ActionCard(
        "＋  Create a project",
        "Start a new Android application",
        onCreateProject
    )

    Spacer(Modifier.height(10.dp))

    ActionCard(
        "✦  Open AI Studio",
        "Generate, explain and improve code",
        onOpenAi
    )

    Spacer(Modifier.height(18.dp))
    Text("Development roadmap", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))

    InfoLine("01", "Project workspace", "Local save enabled")
    InfoLine("02", "AI generation and editor", "Planned")
    InfoLine("03", "GitHub cloud build", "Workflow ready")
    InfoLine("04", "Verify and install APK", "Planned")

    Spacer(Modifier.height(20.dp))
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ForgePanel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                label,
                color = ForgeMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(7.dp))

            Text(
                value,
                color = ForgeAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = ForgePanel),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = ForgeMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoLine(
    number: String,
    title: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(number, color = ForgeAccent, fontWeight = FontWeight.Bold)
            Text(title, fontSize = 13.sp)
        }

        Text(status, color = ForgeMuted, fontSize = 11.sp)
    }
}

@Composable
private fun ProjectsScreen(
    projects: List<ForgeProject>,
    onOpen: (ForgeProject) -> Unit,
    onSave: () -> Unit
) {
    var showEditor by remember { mutableStateOf(false) }

    Spacer(Modifier.height(22.dp))

    Text("Projects", fontSize = 28.sp, fontWeight = FontWeight.Bold)

    Text(
        "Your projects are saved on this device.",
        color = ForgeMuted,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(16.dp))

    Button(onClick = { showEditor = true }) {
        Text("＋  New project")
    }

    Spacer(Modifier.height(14.dp))

    if (projects.isEmpty()) {
        Text(
            "No projects yet. Create your first project.",
            color = ForgeMuted
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(projects.size) { index ->
                val project = projects[index]

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(project) },
                    colors = CardDefaults.cardColors(
                        containerColor = ForgePanel
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            project.name,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            project.packageName,
                            color = ForgeMuted,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Open workspace  →",
                            color = ForgeAccent,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        ProjectEditorDialog(
            project = null,
            existingProjects = projects,
            onDismiss = { showEditor = false },
            onSave = { newProject ->
                (projects as MutableList<ForgeProject>).add(newProject)
                onSave()
                showEditor = false
            }
        )
    }
}

@Composable
private fun ProjectDetailScreen(
    project: ForgeProject,
    existingProjects: List<ForgeProject>,
    onBack: () -> Unit,
    onUpdate: (ForgeProject) -> Unit,
    onDelete: () -> Unit
) {
    var showEditor by remember(project.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember(project.id) { mutableStateOf(false) }

    Spacer(Modifier.height(22.dp))

    TextButton(onClick = onBack) {
        Text("←  All projects")
    }

    Text(
        project.name,
        fontSize = 27.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(6.dp))

    Text("Project workspace", color = ForgeMuted, fontSize = 13.sp)

    Spacer(Modifier.height(18.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = ForgePanel),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "APPLICATION ID",
                color = ForgeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))
            Text(project.packageName, fontSize = 14.sp)

            Spacer(Modifier.height(16.dp))

            Text(
                "WORKSPACE STATUS",
                color = ForgeMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Project details are stored locally on this device.",
                fontSize = 13.sp
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { showEditor = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Edit project")
    }

    Spacer(Modifier.height(8.dp))

    OutlinedButton(
        onClick = { showDeleteConfirm = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Delete project")
    }

    Spacer(Modifier.height(8.dp))

    Text(
        "Code editor, AI generation and APK build controls are not implemented yet.",
        color = ForgeMuted,
        fontSize = 11.sp
    )

    if (showEditor) {
        ProjectEditorDialog(
            project = project,
            existingProjects = existingProjects,
            onDismiss = { showEditor = false },
            onSave = {
                onUpdate(it)
                showEditor = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${project.name}?") },
            text = {
                Text(
                    "This removes the saved project details from this device. " +
                        "This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProjectEditorDialog(
    project: ForgeProject?,
    existingProjects: List<ForgeProject>,
    onDismiss: () -> Unit,
    onSave: (ForgeProject) -> Unit
) {
    var name by remember(project?.id) {
        mutableStateOf(project?.name ?: "")
    }

    var packageName by remember(project?.id) {
        mutableStateOf(project?.packageName ?: "com.example.newapp")
    }

    val validName = name.trim().length in 2..40
    val validPackage = PackagePattern.matches(packageName.trim())

    val duplicatePackage = existingProjects.any {
        it.packageName.equals(packageName.trim(), ignoreCase = true) &&
            it.id != project?.id
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (project == null) "Create project" else "Edit project")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project name") },
                    singleLine = true,
                    isError = name.isNotBlank() && !validName
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("Package name") },
                    singleLine = true,
                    isError = packageName.isNotBlank() &&
                        (!validPackage || duplicatePackage)
                )

                if (!validName && name.isNotBlank()) {
                    Text(
                        "Use 2–40 characters for the project name.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                if (!validPackage && packageName.isNotBlank()) {
                    Text(
                        "Use a valid package name, e.g. com.example.myapp.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                } else if (duplicatePackage) {
                    Text(
                        "This package name is already used by another project.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = validName && validPackage && !duplicatePackage,
                onClick = {
                    onSave(
                        ForgeProject(
                            id = project?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            packageName = packageName.trim()
                        )
                    )
                }
            ) {
                Text(if (project == null) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun InfoScreen(
    title: String,
    subtitle: String,
    items: List<String>
) {
    Spacer(Modifier.height(22.dp))

    Text(
        title,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(Modifier.height(6.dp))

    Text(
        subtitle,
        color = ForgeMuted,
        fontSize = 13.sp
    )

    Spacer(Modifier.height(20.dp))

    items.forEach { item ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = ForgePanel
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("•", color = ForgeAccent, fontSize = 18.sp)
                Text(item, fontSize = 13.sp)
            }
        }
    }
}
