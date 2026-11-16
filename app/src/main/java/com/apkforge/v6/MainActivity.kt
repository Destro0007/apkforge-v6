
package com.apkforge.v6

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ForgeBackground = Color(0xFF0B1020)
private val ForgePanel = Color(0xFF151D32)
private val ForgeAccent = Color(0xFF65D6C4)
private val ForgeMuted = Color(0xFF9AA8C2)

private enum class ForgeTab(val title: String, val symbol: String) {
    HOME("Home", "⌂"),
    PROJECTS("Projects", "▦"),
    AI("AI Studio", "✦"),
    BUILDS("Builds", "↗"),
    SETTINGS("Settings", "⚙")
}

private data class ForgeProject(
    val name: String,
    val packageName: String
)

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
    var selectedTab by remember { mutableStateOf(ForgeTab.HOME) }

    val projects = remember {
        mutableStateListOf(
            ForgeProject("My First App", "com.example.myfirstapp")
        )
    }

    Scaffold(
        containerColor = ForgeBackground,
        bottomBar = {
            NavigationBar(containerColor = ForgePanel) {
                ForgeTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Text(tab.symbol, fontSize = 19.sp) },
                        label = { Text(tab.title, fontSize = 10.sp) }
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
                    },
                    onOpenAi = {
                        selectedTab = ForgeTab.AI
                    }
                )

                ForgeTab.PROJECTS -> ProjectsScreen(projects)

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
                        "Offline workspace: planned",
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

    InfoLine("01", "Project workspace", "In progress")
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
private fun ProjectsScreen(projects: MutableList<ForgeProject>) {
    var showCreate by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("com.example.newapp") }

    Spacer(Modifier.height(22.dp))

    Text("Projects", fontSize = 28.sp, fontWeight = FontWeight.Bold)

    Text("Your app workspace", color = ForgeMuted, fontSize = 13.sp)

    Spacer(Modifier.height(16.dp))

    Button(onClick = { showCreate = true }) {
        Text("＋  New project")
    }

    Spacer(Modifier.height(14.dp))

    if (projects.isEmpty()) {
        Text("No projects yet. Create your first project.", color = ForgeMuted)
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(projects.size) { index ->
                val project = projects[index]

                Card(
                    colors = CardDefaults.cardColors(containerColor = ForgePanel),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(project.name, fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(4.dp))

                        Text(
                            project.packageName,
                            color = ForgeMuted,
                            fontSize = 11.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Workspace created • Build not started",
                            color = ForgeAccent,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Create project") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project name") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = packageName,
                        onValueChange = { packageName = it },
                        label = { Text("Package name") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = projectName.isNotBlank() &&
                        packageName.matches(
                            Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")
                        ),
                    onClick = {
                        projects.add(
                            ForgeProject(
                                projectName.trim(),
                                packageName.trim()
                            )
                        )

                        projectName = ""
                        packageName = "com.example.newapp"
                        showCreate = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showCreate = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoScreen(
    title: String,
    subtitle: String,
    items: List<String>
) {
    Spacer(Modifier.height(22.dp))

    Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)

    Spacer(Modifier.height(6.dp))

    Text(subtitle, color = ForgeMuted, fontSize = 13.sp)

    Spacer(Modifier.height(20.dp))

    items.forEach { item ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = ForgePanel),
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
