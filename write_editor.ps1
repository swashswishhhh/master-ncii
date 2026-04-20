
$enc = [System.Text.UTF8Encoding]::new($false)
$base = $PSScriptRoot

# Mission Editor layout
$editorLayout = @'
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#FF0D1117">

    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/appBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#FF161B22"
        app:elevation="0dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal"
            android:paddingHorizontal="16dp"
            android:paddingTop="16dp"
            android:paddingBottom="12dp">

            <TextView
                android:id="@+id/btnBack"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:fontFamily="monospace"
                android:padding="4dp"
                android:text="&#9664; BACK"
                android:textColor="#FF58A6FF"
                android:textSize="12sp" />

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_marginStart="12dp"
                android:layout_weight="1"
                android:orientation="vertical">

                <TextView
                    android:id="@+id/tvEditorLabel"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:letterSpacing="0.1"
                    android:text="MISSION EDITOR"
                    android:textColor="#FF58A6FF"
                    android:textSize="10sp" />

                <TextView
                    android:id="@+id/tvMissionName"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:fontFamily="monospace"
                    android:text="New Mission"
                    android:textColor="#FFC9D1D9"
                    android:textSize="16sp"
                    android:textStyle="bold" />
            </LinearLayout>

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnPublish"
                android:layout_width="wrap_content"
                android:layout_height="36dp"
                android:enabled="false"
                android:fontFamily="monospace"
                android:insetTop="0dp"
                android:insetBottom="0dp"
                android:text="PUBLISH"
                android:textAllCaps="false"
                android:textColor="#FF0D1117"
                android:textSize="12sp"
                android:textStyle="bold"
                app:backgroundTint="#FF3FB950"
                app:cornerRadius="6dp" />
        </LinearLayout>

        <View
            android:layout_width="match_parent"
            android:layout_height="1dp"
            android:alpha="0.3"
            android:background="#FF58A6FF" />
    </com.google.android.material.appbar.AppBarLayout>

    <ScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fillViewport="true"
        android:overScrollMode="never"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:paddingBottom="24dp">

            <!-- Step indicator -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingHorizontal="16dp"
                android:paddingTop="16dp"
                android:paddingBottom="8dp">

                <TextView
                    android:id="@+id/tvStep1Tab"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_admin_tab_active"
                    android:fontFamily="monospace"
                    android:gravity="center"
                    android:text="1. METADATA"
                    android:textColor="#FF58A6FF"
                    android:textSize="11sp"
                    android:textStyle="bold" />

                <View android:layout_width="8dp" android:layout_height="1dp" />

                <TextView
                    android:id="@+id/tvStep2Tab"
                    android:layout_width="0dp"
                    android:layout_height="36dp"
                    android:layout_weight="1"
                    android:background="@drawable/bg_admin_tab_idle"
                    android:fontFamily="monospace"
                    android:gravity="center"
                    android:text="2. QUESTIONS"
                    android:textColor="#FF8B949E"
                    android:textSize="11sp" />
            </LinearLayout>

            <!-- STEP 1: Mission Metadata -->
            <LinearLayout
                android:id="@+id/stepMetadata"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:paddingHorizontal="16dp"
                android:paddingTop="8dp">

                <com.google.android.material.textfield.TextInputLayout
                    android:id="@+id/tilTitle"
                    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="12dp"
                    android:hint="Mission Title"
                    app:boxBackgroundColor="#FF0D1117"
                    app:boxStrokeColor="#FF30363D"
                    app:hintTextColor="#FF8B949E">

                    <com.google.android.material.textfield.TextInputEditText
                        android:id="@+id/etTitle"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:fontFamily="monospace"
                        android:textColor="#FFC9D1D9"
                        android:textSize="14sp" />
                </com.google.android.material.textfield.TextInputLayout>

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="8dp"
                    android:fontFamily="monospace"
                    android:letterSpacing="0.1"
                    android:text="DIFFICULTY"
                    android:textColor="#FF8B949E"
                    android:textSize="10sp" />

                <RadioGroup
                    android:id="@+id/rgDifficulty"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="16dp"
                    android:orientation="horizontal">

                    <RadioButton
                        android:id="@+id/rbBeginner"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:buttonTint="#FF58A6FF"
                        android:checked="true"
                        android:fontFamily="monospace"
                        android:text="Beginner"
                        android:textColor="#FFC9D1D9"
                        android:textSize="13sp" />

                    <RadioButton
                        android:id="@+id/rbIntermediate"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:buttonTint="#FF58A6FF"
                        android:fontFamily="monospace"
                        android:text="Intermediate"
                        android:textColor="#FFC9D1D9"
                        android:textSize="13sp" />

                    <RadioButton
                        android:id="@+id/rbAdvanced"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:buttonTint="#FF58A6FF"
                        android:fontFamily="monospace"
                        android:text="Advanced"
                        android:textColor="#FFC9D1D9"
                        android:textSize="13sp" />
                </RadioGroup>

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="8dp"
                    android:fontFamily="monospace"
                    android:letterSpacing="0.1"
                    android:text="GAME MECHANIC TYPE"
                    android:textColor="#FF8B949E"
                    android:textSize="10sp" />

                <RadioGroup
                    android:id="@+id/rgMechanic"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginBottom="20dp"
                    android:orientation="horizontal">

                    <RadioButton
                        android:id="@+id/rbOrganizer"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:buttonTint="#FF58A6FF"
                        android:checked="true"
                        android:fontFamily="monospace"
                        android:text="Organizer"
                        android:textColor="#FFC9D1D9"
                        android:textSize="13sp" />

                    <RadioButton
                        android:id="@+id/rbMonitor"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:buttonTint="#FF58A6FF"
                        android:fontFamily="monospace"
                        android:text="Monitor"
                        android:textColor="#FFC9D1D9"
                        android:textSize="13sp" />

                    <RadioButton
                        android:id="@+id/rbMatrix"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:buttonTint="#FF58A6FF"
                        android:fontFamily="monospace"
                        android:text="Matrix"
                        android:textColor="#FFC9D1D9"
                        android:textSize="13sp" />
                </RadioGroup>

                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnNextToQuestions"
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:fontFamily="monospace"
                    android:text="NEXT: ADD QUESTIONS &#9654;"
                    android:textAllCaps="false"
                    android:textColor="#FF0D1117"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    app:backgroundTint="#FF58A6FF"
                    app:cornerRadius="8dp" />
            </LinearLayout>

            <!-- STEP 2: Question Builder -->
            <LinearLayout
                android:id="@+id/stepQuestions"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:visibility="gone">

                <!-- Validation summary bar -->
                <LinearLayout
                    android:id="@+id/validationBar"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:background="#FF161B22"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:paddingHorizontal="16dp"
                    android:paddingVertical="10dp">

                    <View
                        android:id="@+id/validationIndicator"
                        android:layout_width="8dp"
                        android:layout_height="8dp"
                        android:layout_marginEnd="10dp"
                        android:background="@drawable/bg_validation_dot_invalid" />

                    <TextView
                        android:id="@+id/tvValidationStatus"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:fontFamily="monospace"
                        android:text="Complete all questions to enable Publish"
                        android:textColor="#FF8B949E"
                        android:textSize="11sp" />
                </LinearLayout>

                <!-- Question list container -->
                <LinearLayout
                    android:id="@+id/questionContainer"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:paddingTop="8dp" />

                <!-- Add Question button -->
                <com.google.android.material.button.MaterialButton
                    android:id="@+id/btnAddQuestion"
                    style="@style/Widget.Material3.Button.OutlinedButton"
                    android:layout_width="match_parent"
                    android:layout_height="48dp"
                    android:layout_marginHorizontal="16dp"
                    android:layout_marginTop="8dp"
                    android:fontFamily="monospace"
                    android:text="+ ADD QUESTION"
                    android:textAllCaps="false"
                    android:textColor="#FF58A6FF"
                    android:textSize="13sp"
                    app:strokeColor="#FF30363D" />
            </LinearLayout>
        </LinearLayout>
    </ScrollView>

    <!-- Loading overlay -->
    <FrameLayout
        android:id="@+id/loadingOverlay"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#AA0D1117"
        android:visibility="gone">

        <LinearLayout
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center"
            android:gravity="center"
            android:orientation="vertical">

            <ProgressBar
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:indeterminateTint="#FF58A6FF" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:fontFamily="monospace"
                android:text="PUBLISHING..."
                android:textColor="#FF58A6FF"
                android:textSize="12sp" />
        </LinearLayout>
    </FrameLayout>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
'@
[System.IO.File]::WriteAllText("$base\app\src\main\res\layout\activity_admin_mission_editor.xml", $editorLayout, $enc)
Write-Host "Editor layout written"
