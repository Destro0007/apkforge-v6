
package com.apkforge.v6

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val WorkspacePanel = Color(0xFF151D32)
private val WorkspaceAccent = Color(0xFF65D6C4)
private val WorkspaceMuted = Color(0xFF9AA8C2)

private val AllowedExtensions = setOf(
    "kt", "kts", "xml", "json", "md", "txt",
    "gradle", "properties", "yml", "yaml",
    "html", "css", "js"
)

private class ProjectFileStore(
    context: Context,
    projectId: String
) {
    private val root = File(
        context.filesDir,
        "apkforge_projects/" +
            projectId.replace(Regex("[^A-Za-z0-9_-]"), "")
    )

    fun ensureStarterFiles(
        projectName: String,
        packageName: String
    ): List<String> {
        if (!root.exists()) root.mkdirs()

        val readme = File(root, "README.md")
        if (!readme.exists()) {
            readme.writeText(
                "# $projectName\n\n" +
                    "Created with APKForge V6.\n\n" +
                    "Package: $packageName\n\n" +
                    "Project source files are stored locally on this device.\n"
            )
        }

        val main = File(root, "MainActivity.kt")
        if (!main.exists()) {
            main.writeText(
                "package $packageName\n\n" +
                    "import android.os.Bundle\n" +
                    "import androidx.activity.ComponentActivity\n\n" +
                    "class MainActivity : ComponentActivity() {\n" +
                    "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                    "        super.onCreate(savedInstanceState)\n" +
                    "        // Start building your app here.\n" +
                    "    }\n" +
                    "}\n"
            )
        }

        return listFiles()
    }

    fun listFiles(): List<String> {
        if (!root.exists()) return emptyList()

        return root.listFiles()
            ?.filter {
                it.isFile &&
                    it.extension.lowercase() in AllowedExtensions
            }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    fun read(name: String): String {
        val file = safeFile(name) ?: return ""
        return if (file.isFile) file.readText() else ""
    }

    fun write(name: String, content: String) {
        val file = safeFile(name)
            ?: throw IllegalArgumentException("Invalid file name")

        if (!root.exists()) root.mkdirs()
        file.writeText(content)
    }

    fun create(name: String) {
        val file = safeFile(name)
            ?: throw IllegalArgumentException("Invalid file name")

        if (file.exists()) {
            throw IllegalArgumentException("A file with that name already exists")
        }

        if (!root.exists()) root.mkdirs()
        file.writeText("")
    }

    fun delete(name: String) {
        val file = safeFile(name)
            ?: throw IllegalArgumentException("Invalid file name")

        if (file.exists() && !file.delete()) {
            throw IllegalStateException("Could not delete the file")
        }
    }

    private fun safeFile(name: String): File? {
        if (name.isBlank() || name.length > 64) return null
        if (!name.matches(Regex("[A-Za-z0-9_.-]+"))) return null
        if (name.startsWith(".") || name.endsWith(".")) return null

        val extension = name.substringAfterLast(".", "").lowercase()
        if (extension !in AllowedExtensions) return null

        val candidate = File(root, name).canonicalFile
        val canonicalRoot = root.canonicalFile

        return if (candidate.parentFile == canonicalRoot) {
            candidate
        } else {
            null
        }
    }
}

@Composable
internal fun ProjectWorkspaceScreen(
    projectId: String,
    projectName: String,
    packageName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val store = remember(projectId) {
        ProjectFileStore(context, projectId)
    }

    val files = remember(projectId) {
        mutableStateListOf<String>().apply {
            addAll(store.ensureStarterFiles(projectName, packageName))
        }
    }

    var selectedFile by remember(projectId) {
        mutableStateOf(files.firstOrNull())
    }

    var editorText by remember(projectId) {
        mutableStateOf(selectedFile?.let { store.read(it) } ?: "")
    }

    var showNewFileDialog by remember {
        mutableStateOf(false)
    }

    var newFileName by remember {
        mutableStateOf("NewFile.kt")
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    var statusMessage by remember {
        mutableStateOf("Files are stored privately on this device.")
    }

    var hasUnsavedChanges by remember {
        mutableStateOf(false)
    }

    fun openFile(name: String) {
        if (hasUnsavedChanges) {
            val previous = selectedFile
            if (previous != null) {
                try {
                    store.write(previous, editorText)
                } catch (error: Exception) {
                    statusMessage = error.message ?: "Could not save current file"
                    return
                }
            }
        }

        selectedFile = name
        editorText = store.read(name)
        hasUnsavedChanges = false
        statusMessage = "Opened $name"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp, bottom = 8.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("←  Back to project")
        }

        Text(
            projectName,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            packageName,
            color = WorkspaceMuted,
            fontSize = 11.sp
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "PROJECT FILES",
                color = WorkspaceAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(onClick = {
                showNewFileDialog = true
            }) {
                Text("+ New file")
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 148.dp),
            colors = CardDefaults.cardColors(
                containerColor = WorkspacePanel
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (files.isEmpty()) {
                Text(
                    "No files yet. Create a file to begin.",
                    color = WorkspaceMuted,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 12.sp
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(6.dp)
                ) {
                    items(files, key = { it }) { fileName ->
                        val selected = selectedFile == fileName

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) Color(0xFF23344A)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    openFile(fileName)
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 10.dp
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "▤  $fileName",
                                color = if (selected) WorkspaceAccent
                                else Color.White,
                                fontSize = 13.sp
                            )

                            if (selected) {
                                Text(
                                    "OPEN",
                                    color = WorkspaceMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                selectedFile ?: "No file selected",
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            if (hasUnsavedChanges) {
                Text(
                    "UNSAVED",
                    color = Color(0xFFFFC36B),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = editorText,
            onValueChange = {
                editorText = it
                hasUnsavedChanges = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            enabled = selectedFile != null,
            placeholder = {
                Text("Select a file or create a new one")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrect = false
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val current = selectedFile
                    if (current != null) {
                        try {
                            store.write(current, editorText)
                            hasUnsavedChanges = false
                            statusMessage = "Saved $current"
                        } catch (error: Exception) {
                            statusMessage =
                                error.message ?: "Save failed"
                        }
                    }
                },
                enabled = selectedFile != null && hasUnsavedChanges,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save file")
            }

            OutlinedButton(
                onClick = {
                    showDeleteDialog = true
                },
                enabled = selectedFile != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Delete file")
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            statusMessage,
            color = WorkspaceMuted,
            fontSize = 10.sp
        )
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = {
                showNewFileDialog = false
            },
            title = {
                Text("Create project file")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = {
                            newFileName = it
                        },
                        label = {
                            Text("File name")
                        },
                        singleLine = true
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Supported: .kt, .kts, .xml, .json, .md, .txt, .gradle, .properties, .yml, .yaml, .html, .css, .js",
                        color = WorkspaceMuted,
                        fontSize = 10.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val name = newFileName.trim()
                            store.create(name)

                            val refreshed = store.listFiles()
                            files.clear()
                            files.addAll(refreshed)

                            openFile(name)
                            newFileName = "NewFile.kt"
                            showNewFileDialog = false
                        } catch (error: Exception) {
                            statusMessage =
                                error.message ?: "Could not create file"
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNewFileDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Delete this file?")
            },
            text = {
                Text(
                    "The selected file will be permanently removed from this project."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = selectedFile

                        if (current != null) {
                            try {
                                store.delete(current)

                                val refreshed = store.listFiles()
                                files.clear()
                                files.addAll(refreshed)

                                selectedFile = refreshed.firstOrNull()
                                editorText =
                                    selectedFile?.let { store.read(it) } ?: ""

                                hasUnsavedChanges = false
                                statusMessage = "File deleted"
                            } catch (error: Exception) {
                                statusMessage =
                                    error.message ?: "Delete failed"
                            }
                        }

                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
