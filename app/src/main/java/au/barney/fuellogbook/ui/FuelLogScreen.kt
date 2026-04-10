package au.barney.fuellogbook.ui

import androidx.compose.foundation.clickable
import android.content.Context
import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import au.barney.fuellogbook.data.LogEntry
import au.barney.fuellogbook.data.Vehicle
import androidx.lifecycle.viewModelScope
import au.barney.fuellogbook.util.BackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelLogScreen(viewModel: FuelLogViewModel) {
    var showLogs by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var showVehicleDialog by remember { mutableStateOf(false) }
    var showFuelTypeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var useSpecialIcon by remember { mutableStateOf(prefs.getBoolean("use_special_icon", false)) }
    
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportToCsv(context, it) }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let { viewModel.backupDatabase(context, it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.restoreDatabase(context, it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(if (showLogs) "Fuel Logs" else "Add Fuel Log") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Box {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("Management Options")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    painter = painterResource(
                                        id = if (useSpecialIcon) 
                                            au.barney.fuellogbook.R.drawable.ic_ncc_1701 
                                        else 
                                            au.barney.fuellogbook.R.drawable.ic_fuel_icon
                                    ),
                                    contentDescription = "Management",
                                    modifier = Modifier.size(32.dp),
                                    tint = androidx.compose.ui.graphics.Color.Unspecified
                                )
                            }
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (showLogs) "Add Fuel Log" else "Fuel Logs") },
                                onClick = {
                                    showLogs = !showLogs
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (showLogs) Icons.Default.Add else Icons.Default.List,
                                        contentDescription = null
                                    )
                                }
                            )
                            
                            if (!showLogs) {
                                DropdownMenuItem(
                                    text = { Text("Manage Vehicles") },
                                    onClick = {
                                        showVehicleDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage Fuel Types") },
                                    onClick = {
                                        showFuelTypeDialog = true
                                        showMenu = false
                                    },
                                    leadingIcon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) }
                                )
                            }
                            
                            Divider()
                            
                            DropdownMenuItem(
                                text = { Text("Export to CSV") },
                                onClick = {
                                    exportLauncher.launch("fuel_logs_${System.currentTimeMillis()}.csv")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Backup Database") },
                                onClick = {
                                    backupLauncher.launch("fuel_log_backup_${System.currentTimeMillis()}.db")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Backup, contentDescription = null) }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Restore Database") },
                                onClick = {
                                    restoreLauncher.launch("*/*")
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) }
                            )

                            Divider()

                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    showAboutDialog = true
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (showLogs) {
                LogListScreen(viewModel)
            } else {
                AddLogScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(16.dp),
                    showVehicleDialog = showVehicleDialog,
                    onDismissVehicleDialog = { showVehicleDialog = false },
                    showFuelTypeDialog = showFuelTypeDialog,
                    onDismissFuelTypeDialog = { showFuelTypeDialog = false }
                )
            }

            if (showAboutDialog) {
                AboutDialog(
                    onDismiss = { showAboutDialog = false },
                    onToggle = { useSpecialIcon = prefs.getBoolean("use_special_icon", false) }
                )
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit, onToggle: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var tapCount by remember { mutableIntStateOf(0) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About FuelLogBook") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "-v1.4-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable {
                        tapCount++
                        if (tapCount >= 7) {
                            val current = prefs.getBoolean("use_special_icon", false)
                            prefs.edit().putBoolean("use_special_icon", !current).apply()
                            onToggle()
                            tapCount = 0
                            android.widget.Toast.makeText(context, "Easter Egg Icon found and changed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                Text(
                    "FuelLogBook is a precision vehicle telemetry and compliance tool designed specifically for Australian drivers. It helps you track fuel usage, monitor engine efficiency, and maintain a complete digital record of your vehicle’s running costs.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Accurate Fuel & Distance Tracking", fontWeight = FontWeight.Bold)
                Text(
                    "FuelLogBook automatically calculates fuel economy using the Australian standard L/100km formula. Each new entry inherits your previous odometer reading, ensuring your distance and efficiency data stays accurate over time.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Supports Multiple Vehicles", fontWeight = FontWeight.Bold)
                Text(
                    "You can manage as many vehicles as you need, each organised by its Rego.\nThis makes FuelLogBook ideal for households, enthusiasts, and small business fleets.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Custom Fuel Types", fontWeight = FontWeight.Bold)
                Text(
                    "FuelLogBook doesn’t lock you into a preset list.\nYou can manually add any fuel type you use, including:\n\n" +
                            "• 91 / 95 / 98\n" +
                            "• E10\n" +
                            "• Diesel\n" +
                            "• Premium blends\n" +
                            "• Specialty or regional fuels\n\n" +
                            "Each log entry records the exact fuel type you selected or created, helping you track performance and cost differences over time.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Smart At‑the‑Pump Features", fontWeight = FontWeight.Bold)
                Text(
                    "The app is built for fast, error‑free entry while refuelling.\nYou can enter fuel cost in $/L or c/L, both commonly used across Australia.\nFuelLogBook also validates your Total Cost against your Cost‑Per‑Litre and Litres to help prevent mistakes before saving.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Receipt Scanning & Digital Records", fontWeight = FontWeight.Bold)
                Text(
                    "Using Google’s ML Kit, FuelLogBook includes a professional‑grade document scanner.\nYou can capture and store fuel receipts directly in the app — ideal for ATO logbooks, tax claims, and business reimbursements.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Data Portability & Backup", fontWeight = FontWeight.Bold)
                Text(
                    "Your logs are stored locally using a secure Room database, so the app works fully offline.\nYou can export your history to CSV for accountants or EOFY reporting, and you can back up and restore your entire database when changing devices.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text("Built for Australian Roads", fontWeight = FontWeight.Bold)
                Text(
                    "FuelLogBook is designed for real Australian driving conditions — long distances, varied fuel pricing, and multiple vehicle ownership.\nWhether you’re commuting, towing, or travelling across the country, the app provides a clear, reliable record of your vehicle’s performance.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLogScreen(
    viewModel: FuelLogViewModel,
    modifier: Modifier = Modifier,
    editingLog: LogEntry? = null,
    onLogSaved: () -> Unit = {},
    showVehicleDialog: Boolean = false,
    onDismissVehicleDialog: () -> Unit = {},
    showFuelTypeDialog: Boolean = false,
    onDismissFuelTypeDialog: () -> Unit = {}
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelTypes by viewModel.fuelTypes.collectAsState()

    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var vehicleText by remember { mutableStateOf("") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var odoStart by remember { mutableStateOf("") }
    var odoEnd by remember { mutableStateOf("") }
    var selectedFuelType by remember { mutableStateOf("") }
    var costPerLitreInput by remember { mutableStateOf("") }
    var isCents by remember { mutableStateOf(false) } // Toggle for Cents vs Dollars
    var litres by remember { mutableStateOf("") }
    var totalCostInput by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(editingLog, vehicles) {
        editingLog?.let { log ->
            selectedVehicle = vehicles.find { it.vehID == log.vehicleID }
            vehicleText = selectedVehicle?.vehicle ?: ""
            dateMillis = log.dateOfFill
            odoStart = log.odometerStart.toString().removeSuffix(".0")
            odoEnd = log.odometerEnd.toString().removeSuffix(".0")
            selectedFuelType = log.fuelType
            
            // Format price for edit: $/L -> 2.99, c/L -> 299.9
            if ((log.costPerLitre * 100) % 1.0 > 0.001) {
                isCents = true
                costPerLitreInput = String.format(Locale.getDefault(), "%.1f", log.costPerLitre * 100.0)
            } else {
                isCents = false
                costPerLitreInput = String.format(Locale.getDefault(), "%.2f", log.costPerLitre)
            }            
            litres = log.litres.toString()
            totalCostInput = log.cost.toString()
            notes = log.notes ?: ""
            receiptUri = log.receiptPath?.let { Uri.fromFile(File(it)) }
        }
    }

    // Derived states
    val distance = remember(odoStart, odoEnd) {
        val start = odoStart.toDoubleOrNull() ?: 0.0
        val end = odoEnd.toDoubleOrNull() ?: 0.0
        if (end >= start) end - start else 0.0
    }

    val costPerLitre = remember(costPerLitreInput, isCents) {
        val input = costPerLitreInput.toDoubleOrNull() ?: 0.0
        if (isCents) input / 100.0 else input
    }

    val totalCost = remember(totalCostInput) {
        totalCostInput.toDoubleOrNull() ?: 0.0
    }

    val projectedCost = remember(costPerLitre, litres) {
        val l = litres.toDoubleOrNull() ?: 0.0
        costPerLitre * l
    }

    val consumption = remember(distance, litres) {
        val l = litres.toDoubleOrNull() ?: 0.0
        if (distance > 0) (l / distance) * 100 else 0.0
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> receiptUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pages?.get(0)?.imageUri?.let { uri ->
                receiptUri = uri
            }
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(false)
            .setResultFormats(RESULT_FORMAT_JPEG)
            .setScannerMode(SCANNER_MODE_FULL)
            .setPageLimit(1)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rego and Vehicle on the same line
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Rego Dropdown
            var regoExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedVehicle?.rego ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rego") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { regoExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                DropdownMenu(
                    expanded = regoExpanded,
                    onDismissRequest = { regoExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    vehicles.forEach { vehicle ->
                        DropdownMenuItem(
                            text = { Text(vehicle.rego) },
                            onClick = {
                                selectedVehicle = vehicle
                                vehicleText = vehicle.vehicle
                                regoExpanded = false

                                // Pre-fill Odo Start and Fuel Type from previous log
                                if (editingLog == null) {
                                    scope.launch {
                                        viewModel.getLatestLogForVehicle(vehicle.vehID)?.let { latestLog ->
                                            odoStart = latestLog.odometerEnd.toString().removeSuffix(".0")
                                            selectedFuelType = latestLog.fuelType
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Vehicle Name
            OutlinedTextField(
                value = vehicleText,
                onValueChange = { vehicleText = it },
                label = { Text("Vehicle") },
                modifier = Modifier.weight(1f),
                readOnly = true
            )
        }

        // Date Picker
        OutlinedTextField(
            value = dateFormatter.format(Date(dateMillis)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                }
            }
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        dateMillis = datePickerState.selectedDateMillis ?: dateMillis
                        showDatePicker = false
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Odometers
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = odoStart,
                onValueChange = { odoStart = it },
                label = { Text("Odo Start") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = odoEnd,
                onValueChange = { odoEnd = it },
                label = { Text("Odo End") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Text("Distance: ${String.format(Locale.getDefault(), "%.1f", distance)} km", style = MaterialTheme.typography.bodyLarge)

        // Fuel Type and Total Cost on the same line
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            // Fuel Type Dropdown
            var fuelExpanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1.2f)) {
                OutlinedTextField(
                    value = selectedFuelType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fuel Type") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { fuelExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                )
                DropdownMenu(
                    expanded = fuelExpanded,
                    onDismissRequest = { fuelExpanded = false }
                ) {
                    fuelTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.fuelType) },
                            onClick = {
                                selectedFuelType = type.fuelType
                                fuelExpanded = false
                            }
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = totalCostInput,
                    onValueChange = { totalCostInput = it },
                    label = { Text("Total Cost") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                if (projectedCost > 0) {
                    Text(
                        text = "Proj: AU$ ${String.format(Locale.getDefault(), "%.2f", projectedCost)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (Math.abs(projectedCost - totalCost) > 0.01) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }
        }

        // Cost and Litres on the same line
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            var isPriceFocused by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = costPerLitreInput,
                onValueChange = { newValue ->
                    // Reverted Price per L logic to the auto-insert dot version
                    val digits = newValue.filter { it.isDigit() }
                    if (isCents) {
                        // c/L: XXX.X
                        if (digits.length >= 4) {
                            costPerLitreInput = digits.substring(0, 3) + "." + digits.substring(3, 4)
                        } else {
                            costPerLitreInput = newValue
                        }
                    } else {
                        // $/L: X.XX
                        if (digits.length >= 3) {
                            costPerLitreInput = digits.substring(0, 1) + "." + digits.substring(1, 3)
                        } else {
                            costPerLitreInput = newValue
                        }
                    }
                },
                label = { 
                    Text(
                        if (isPriceFocused || costPerLitreInput.isNotEmpty()) 
                            if (isCents) "Price (c/L)" else "Price ($/L)" 
                        else "Price"
                    ) 
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isPriceFocused = it.isFocused },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    FilledTonalButton(
                        onClick = {
                            val currentPrice = costPerLitreInput.toDoubleOrNull()
                            if (currentPrice != null) {
                                costPerLitreInput = if (isCents) {
                                    // Switching from cents to dollars: 299.9 -> 2.99
                                    String.format(Locale.getDefault(), "%.2f", currentPrice / 100.0)
                                } else {
                                    // Switching from dollars to cents: 2.99 -> 299.9
                                    String.format(Locale.getDefault(), "%.1f", currentPrice * 100.0)
                                }
                            }
                            isCents = !isCents
                        },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (isCents) "c/L" else "$/L",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            OutlinedTextField(
                value = litres,
                onValueChange = { litres = it },
                label = { Text("Litres") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        // Totals
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Consumption: ${String.format(Locale.getDefault(), "%.2f", consumption)} L/100km")
            }
        }

        // Notes section
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        )

        // Receipt Selection
        var showReceiptSourceDialog by remember { mutableStateOf(false) }

        if (showReceiptSourceDialog) {
            AlertDialog(
                onDismissRequest = { showReceiptSourceDialog = false },
                title = { Text("Add Receipt") },
                text = { Text("Choose a source for the receipt image") },
                confirmButton = {
                    TextButton(onClick = {
                        showReceiptSourceDialog = false
                        scanner.getStartScanIntent(context as Activity)
                            .addOnSuccessListener { intentSender ->
                                cameraLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                            }
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Camera")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showReceiptSourceDialog = false
                        imagePickerLauncher.launch("*/*")
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gallery")
                        }
                    }
                }
            )
        }

        Button(
            onClick = { showReceiptSourceDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.AddAPhoto, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Add Receipt")
        }

        Button(
            onClick = {
                var savedReceiptPath: String? = null
                receiptUri?.let { uri ->
                    try {
                        // Avoid re-copying if it's already in our storage (editing)
                        if (uri.scheme == "file" && uri.path?.contains(context.filesDir.path) == true) {
                            savedReceiptPath = uri.path
                        } else {
                            val fileName = "receipt_${System.currentTimeMillis()}.jpg"
                            val file = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
                            val destFile = File(file, fileName)
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            savedReceiptPath = destFile.absolutePath
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val log = LogEntry(
                    logID = editingLog?.logID ?: 0,
                    vehicleID = selectedVehicle?.vehID ?: 0,
                    dateOfFill = dateMillis,
                    odometerStart = odoStart.toDoubleOrNull() ?: 0.0,
                    odometerEnd = odoEnd.toDoubleOrNull() ?: 0.0,
                    distance = distance,
                    fuelType = selectedFuelType,
                    costPerLitre = costPerLitre,
                    litres = litres.toDoubleOrNull() ?: 0.0,
                    cost = totalCost,
                    litersPer100 = consumption,
                    receiptPath = savedReceiptPath ?: editingLog?.receiptPath,
                    notes = notes
                )
                if (editingLog != null) {
                    viewModel.updateLog(log)
                } else {
                    viewModel.addLog(log)
                }
                onLogSaved()
                
                // Reset fields if not editing
                if (editingLog == null) {
                    selectedVehicle = null
                    vehicleText = ""
                    odoStart = ""
                    odoEnd = ""
                    selectedFuelType = ""
                    costPerLitreInput = ""
                    totalCostInput = ""
                    litres = ""
                    notes = ""
                    receiptUri = null
                    // dateMillis = System.currentTimeMillis() // Optional: reset date too?
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedVehicle != null && selectedFuelType.isNotEmpty() && distance > 0 && litres.isNotEmpty() && totalCost > 0 && costPerLitre > 0
        ) {
            Text(if (editingLog != null) "Update Log Entry" else "Save Log Entry")
        }

        receiptUri?.let { uri ->
            Card(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                onClick = { /* Could open full screen here too */ }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Receipt Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = { receiptUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = androidx.compose.ui.graphics.Color.White)
                    }
                }
            }


        }
        
        // Ensure content clears the navigation bar
        Spacer(Modifier.navigationBarsPadding())
    }

    // Vehicle Management Dialog
    if (showVehicleDialog) {
        var newRego by remember { mutableStateOf("") }
        var newName by remember { mutableStateOf("") }
        var showInUseMessage by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissVehicleDialog,
            title = { Text("Manage Vehicles") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showInUseMessage) {
                        Text(
                            "Cannot delete vehicle because it is being used in log entries.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = newRego,
                        onValueChange = { 
                            newRego = it
                            showInUseMessage = false
                        },
                        label = { Text("Rego") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { 
                            newName = it
                            showInUseMessage = false
                        },
                        label = { Text("Vehicle Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        if (newRego.isNotBlank() && newName.isNotBlank()) {
                            viewModel.addVehicle(newRego, newName)
                            newRego = ""
                            newName = ""
                        }
                    }) { Text("Add Vehicle") }
                    
                    Divider()
                    Text("Existing Vehicles:", style = MaterialTheme.typography.labelLarge)
                    vehicles.forEach { vehicle ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${vehicle.rego} - ${vehicle.vehicle}")
                            IconButton(onClick = {
                                viewModel.deleteVehicle(vehicle, onInUse = {
                                    showInUseMessage = true
                                })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissVehicleDialog) { Text("Close") } }
        )
    }

    // Fuel Type Management Dialog
    if (showFuelTypeDialog) {
        var newFuelType by remember { mutableStateOf("") }
        var showInUseMessage by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = onDismissFuelTypeDialog,
            title = { Text("Manage Fuel Types") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showInUseMessage) {
                        Text(
                            "Cannot delete fuel type because it is being used in log entries.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    OutlinedTextField(
                        value = newFuelType,
                        onValueChange = { 
                            newFuelType = it
                            showInUseMessage = false
                        },
                        label = { Text("Fuel Type Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = {
                        if (newFuelType.isNotBlank()) {
                            viewModel.addFuelType(newFuelType)
                            newFuelType = ""
                        }
                    }) { Text("Add Fuel Type") }
                    
                    Divider()
                    Text("Existing Fuel Types:", style = MaterialTheme.typography.labelLarge)
                    fuelTypes.forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.fuelType)
                            IconButton(onClick = {
                                viewModel.deleteFuelType(type, onInUse = {
                                    showInUseMessage = true
                                })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissFuelTypeDialog) { Text("Close") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogListScreen(viewModel: FuelLogViewModel) {
    val logs by viewModel.filteredLogs.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val fuelTypes by viewModel.fuelTypes.collectAsState()
    val filterVehicleId by viewModel.filterVehicleId.collectAsState()
    val filterFuelType by viewModel.filterFuelType.collectAsState()
    val filterDateFrom by viewModel.filterDateFrom.collectAsState()
    val filterDateTo by viewModel.filterDateTo.collectAsState()

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val filterDateFormatter = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }

    var editingLog by remember { mutableStateOf<LogEntry?>(null) }

    if (editingLog != null) {
        Dialog(
            onDismissRequest = { editingLog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.95f),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit Log Entry", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { editingLog = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    AddLogScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(top = 8.dp),
                        editingLog = editingLog,
                        onLogSaved = { editingLog = null }
                    )
                }
            }
        }
    }

    var showDateRangePicker by remember { mutableStateOf(false) }
    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDateFilter(
                            dateRangePickerState.selectedStartDateMillis,
                            dateRangePickerState.selectedEndDateMillis
                        )
                        showDateRangePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.setDateFilter(null, null)
                        showDateRangePicker = false
                    }
                ) { Text("Clear") }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                modifier = Modifier.weight(1f)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filters
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Vehicle Filter
                var vehicleExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(onClick = { vehicleExpanded = true }) {
                        val selectedVehicle = vehicles.find { it.vehID == filterVehicleId }
                        Text(
                            text = selectedVehicle?.rego ?: "All Vehicles",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    DropdownMenu(expanded = vehicleExpanded, onDismissRequest = { vehicleExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Vehicles") }, onClick = {
                            viewModel.setVehicleFilter(null)
                            vehicleExpanded = false
                        })
                        vehicles.forEach { vehicle ->
                            DropdownMenuItem(text = { Text(vehicle.rego) }, onClick = {
                                viewModel.setVehicleFilter(vehicle.vehID)
                                vehicleExpanded = false
                            })
                        }
                    }
                }

                // Fuel Type Filter
                var fuelExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(onClick = { fuelExpanded = true }) {
                        Text(
                            text = filterFuelType ?: "All Fuel",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    DropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                        DropdownMenuItem(text = { Text("All Fuel") }, onClick = {
                            viewModel.setFuelTypeFilter(null)
                            fuelExpanded = false
                        })
                        fuelTypes.forEach { type ->
                            DropdownMenuItem(text = { Text(type.fuelType) }, onClick = {
                                viewModel.setFuelTypeFilter(type.fuelType)
                                fuelExpanded = false
                            })
                        }
                    }
                }
            }

            // Date Filter & Clear button
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedCard(onClick = { showDateRangePicker = true }) {
                        val dateText = if (filterDateFrom != null && filterDateTo != null) {
                            "${filterDateFormatter.format(Date(filterDateFrom!!))} - ${filterDateFormatter.format(Date(filterDateTo!!))}"
                        } else {
                            "All Dates"
                        }
                        Text(
                            text = dateText,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                
                Button(
                    onClick = {
                        viewModel.setVehicleFilter(null)
                        viewModel.setFuelTypeFilter(null)
                        viewModel.setDateFilter(null, null)
                    },
                    modifier = Modifier.weight(0.6f),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Clear Filters", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp)
        ) {
            items(logs) { log ->
                val vehicle = vehicles.find { it.vehID == log.vehicleID }
                var showDeleteDialog by remember { mutableStateOf(false) }
                var showContextMenu by remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Log Entry") },
                        text = { Text("Are you sure you want to delete this log entry?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteLog(log.logID)
                                showDeleteDialog = false
                            }) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onLongClick = { showContextMenu = true },
                            onClick = {}
                        )
                ) {
                    Box {
                        DropdownMenu(
                            expanded = showContextMenu,
                            onDismissRequest = { showContextMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showContextMenu = false
                                    editingLog = log
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = {
                                    showContextMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = vehicle?.rego ?: "Unknown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = dateFormatter.format(Date(log.dateOfFill)))
                            }
                            Text("${vehicle?.vehicle ?: ""}")
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Type: ${log.fuelType}")
                                    val priceFormatted = if (log.costPerLitre % 1.0 == 0.0) {
                                        String.format("%.0f", log.costPerLitre)
                                    } else if ((log.costPerLitre * 10) % 1.0 == 0.0) {
                                        String.format("%.1f", log.costPerLitre)
                                    } else {
                                        String.format("%.2f", log.costPerLitre)
                                    }
                                    Text("Price: AU$ $priceFormatted/L")
                                    Text("Distance: ${String.format("%.1f", log.distance)} km")
                                    if (log.receiptPath != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        var showReceiptFull by remember { mutableStateOf(false) }
                                        if (showReceiptFull) {
                                            AlertDialog(
                                                onDismissRequest = { showReceiptFull = false },
                                                text = {
                                                    AsyncImage(
                                                        model = File(log.receiptPath),
                                                        contentDescription = "Receipt",
                                                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                                    )
                                                },
                                                confirmButton = { TextButton(onClick = { showReceiptFull = false }) { Text("Close") } }
                                            )
                                        }
                                        IconButton(onClick = { showReceiptFull = true }) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = "View Receipt")
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Cost: AU$ ${String.format("%.2f", log.cost)}")
                                    Text("L/100km: ${String.format("%.2f", log.litersPer100)}")
                                    Text("${String.format("%.2f", log.litres)} L")
                                }
                            }
                            if (!log.notes.isNullOrBlank()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Notes: ${log.notes}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }


        }
    }
}
