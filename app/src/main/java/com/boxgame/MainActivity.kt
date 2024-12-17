package com.boxgame

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.orhanobut.hawk.Hawk
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Hawk.init(this).build()

        setContent {
            GameScreen()
        }
    }
}

@Composable
fun GameScreen() {
    val boxSize = 40f
    val enemySize = 60f
    val borderWidth = 40f

    val screenWidth: Float
    val screenHeight: Float

    with(LocalDensity.current) {
        screenWidth = LocalConfiguration.current.screenWidthDp.dp.toPx() - 32.dp.toPx()
        screenHeight = screenWidth // Square field
    }

    var boxPosition by remember { mutableStateOf(Offset((screenWidth - boxSize) / 2, (screenHeight - boxSize) / 2)) }
    var gameOver by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }
    var endTime by remember { mutableStateOf(0L) }
    var enemies = remember { mutableStateListOf<Enemy>() }
    var gameStarted by remember { mutableStateOf(false) }
    var bestTime by remember { mutableFloatStateOf(getBestTime()) }

    // Function to start the clock
    fun startClock() {
        startTime = System.currentTimeMillis()
        gameStarted = true
    }

    // Function to end the clock
    fun endClock() {
        endTime = System.currentTimeMillis()
        gameOver = true
    }

    // Function to calculate the time survived
    fun calcTime(): Float {
        return (endTime - startTime) / 1000f
    }

    // Function to reset the game
    fun resetGame() {
        gameOver = false
        boxPosition = Offset((screenWidth - boxSize) / 2, (screenHeight - boxSize) / 2) // Start in the center
        enemies.clear()
        gameStarted = false

        repeat(4) {
            var enemyPosition: Offset
            do {
                enemyPosition = Offset(
                    Random.nextFloat() * (screenWidth - borderWidth * 2 - enemySize) + borderWidth,
                    Random.nextFloat() * (screenHeight - borderWidth * 2 - enemySize) + borderWidth
                )
            } while (
                enemyPosition.x < boxPosition.x + boxSize &&
                enemyPosition.x + enemySize > boxPosition.x &&
                enemyPosition.y < boxPosition.y + boxSize &&
                enemyPosition.y + enemySize > boxPosition.y
            )

            enemies.add(
                Enemy(
                    position = enemyPosition,
                    velocity = Offset(
                        Random.nextInt(10, 15).toFloat(),
                        Random.nextInt(10, 15).toFloat()
                    )
                )
            )
        }

    }

    LaunchedEffect(Unit) {
        resetGame()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                startClock()
                            },
                            onDragEnd = {
                                endClock()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (!gameOver) {
                                    val newX = (boxPosition.x + dragAmount.x).coerceIn(
                                        borderWidth,
                                        screenWidth - boxSize - borderWidth
                                    )
                                    val newY = (boxPosition.y + dragAmount.y).coerceIn(
                                        borderWidth,
                                        screenHeight - boxSize - borderWidth
                                    )
                                    boxPosition = Offset(newX, newY)
                                }
                            }
                        )
                    }
            ) {
                // Draw border
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(screenWidth, borderWidth)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(0f, screenHeight - borderWidth),
                    size = androidx.compose.ui.geometry.Size(screenWidth, borderWidth)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(0f, borderWidth),
                    size = androidx.compose.ui.geometry.Size(borderWidth, screenHeight - borderWidth * 2)
                )
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(screenWidth - borderWidth, borderWidth),
                    size = androidx.compose.ui.geometry.Size(borderWidth, screenHeight - borderWidth * 2)
                )

                // Draw red box
                drawRect(
                    color = Color.Red,
                    topLeft = boxPosition,
                    size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
                )

                // Draw enemies
                enemies.forEach { enemy ->
                    drawRect(
                        color = Color.Blue,
                        topLeft = enemy.position,
                        size = androidx.compose.ui.geometry.Size(enemySize, enemySize)
                    )
                }
            }

            // Move enemies smoothly towards their target position with reduced frequency
            LaunchedEffect(gameOver) {
                if (!gameOver) {
                    while (true) {
                        enemies.forEach { enemy ->
                            enemy.updatePosition(screenWidth, screenHeight, enemySize, borderWidth)
                        }
                        delay(30L)
                    }
                }
            }

            LaunchedEffect(boxPosition) {
                if (!gameOver) {
                    enemies.forEach { enemy ->
                        if (boxPosition.x < enemy.position.x + enemySize &&
                            boxPosition.x + boxSize > enemy.position.x &&
                            boxPosition.y < enemy.position.y + enemySize &&
                            boxPosition.y + boxSize > enemy.position.y
                        ) {
                            gameOver = true
                        }
                    }
                }
            }

            if (gameOver) {
                LaunchedEffect(Unit) {
                    val timeSurvived = calcTime()
                    if (timeSurvived > bestTime) {
                        bestTime = timeSurvived
                        saveBestTime(bestTime)
                        Log.d("GameBox", "Current Best Time: $bestTime")
                    }
                }

                AlertDialog(
                    onDismissRequest = { resetGame() },
                    title = { Text("Game Over") },
                    text = { Text("You survived for ${"%.2f".format(calcTime())} seconds.") },
                    confirmButton = {
                        Button(onClick = { resetGame() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }

        // Record times display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Best Time: ${"%.2f".format(bestTime)}s")
        }
    }
}

data class Enemy(var position: Offset, var velocity: Offset) {
    suspend fun updatePosition(screenWidth: Float, screenHeight: Float, size: Float, borderWidth: Float) {
        val newX = position.x + velocity.x
        val newY = position.y + velocity.y

        position = Offset(
            x = when {
                newX < borderWidth -> borderWidth.also { velocity = velocity.copy(x = -velocity.x) }
                newX + size > screenWidth - borderWidth -> (screenWidth - borderWidth - size).also {
                    velocity = velocity.copy(x = -velocity.x)
                }

                else -> newX
            },
            y = when {
                newY < borderWidth -> borderWidth.also { velocity = velocity.copy(y = -velocity.y) }
                newY + size > screenHeight - borderWidth -> (screenHeight - borderWidth - size).also {
                    velocity = velocity.copy(y = -velocity.y)
                }

                else -> newY
            }
        )
    }
}
