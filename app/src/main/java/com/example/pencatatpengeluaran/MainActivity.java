package com.example.pencatatpengeluaran;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PrefsManager prefs;        // SharedPreferences
    private DatabaseHelper db;         // SQLite

    private TextView tvGreeting, tvTotal, tvBudgetInfo, tvEmpty;
    private LinearProgressIndicator progressBudget;
    private MaterialCardView panelSettings;
    private TextInputEditText etName, etBudget, etDesc, etAmount;
    private TextInputLayout tilDesc, tilAmount;

    private ExpenseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // [SharedPreferences] terapkan tema tersimpan sebelum layout dibuat
        prefs = new PrefsManager(this);
        AppCompatDelegate.setDefaultNightMode(prefs.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);

        // Header (ringkasan, pengaturan, form) dipasang sebagai header ListView
        ListView lvExpenses = findViewById(R.id.lvExpenses);
        View header = getLayoutInflater().inflate(R.layout.header_main, lvExpenses, false);
        lvExpenses.addHeaderView(header, null, false);

        tvGreeting = header.findViewById(R.id.tvGreeting);
        tvTotal = header.findViewById(R.id.tvTotal);
        tvBudgetInfo = header.findViewById(R.id.tvBudgetInfo);
        tvEmpty = header.findViewById(R.id.tvEmpty);
        progressBudget = header.findViewById(R.id.progressBudget);
        panelSettings = header.findViewById(R.id.panelSettings);
        etName = header.findViewById(R.id.etName);
        etBudget = header.findViewById(R.id.etBudget);
        etDesc = header.findViewById(R.id.etDesc);
        etAmount = header.findViewById(R.id.etAmount);
        tilDesc = header.findViewById(R.id.tilDesc);
        tilAmount = header.findViewById(R.id.tilAmount);
        MaterialSwitch switchDark = header.findViewById(R.id.switchDark);
        Button btnSettings = header.findViewById(R.id.btnSettings);
        Button btnSaveSettings = header.findViewById(R.id.btnSaveSettings);
        Button btnAdd = header.findViewById(R.id.btnAdd);
        ChipGroup chipGroup = header.findViewById(R.id.chipGroup);

        adapter = new ExpenseAdapter(this, this::confirmDelete);
        lvExpenses.setAdapter(adapter);

        // Chip cepat: isi kolom keterangan
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            chip.setOnClickListener(v -> {
                etDesc.setText(chip.getText());
                etDesc.setSelection(etDesc.length());
                etAmount.requestFocus();
            });
        }

        // Buka/tutup panel pengaturan
        btnSettings.setOnClickListener(v -> panelSettings.setVisibility(
                panelSettings.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE));

        // [SharedPreferences] switch mode gelap
        switchDark.setChecked(prefs.isDarkMode());
        switchDark.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setDarkMode(isChecked);   // apply()
            AppCompatDelegate.setDefaultNightMode(isChecked
                    ? AppCompatDelegate.MODE_NIGHT_YES
                    : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // [SharedPreferences] simpan nama dan budget
        btnSaveSettings.setOnClickListener(v -> {
            String name = textOf(etName).trim();
            long budget = parseAmount(textOf(etBudget));
            prefs.saveUserName(name);       // apply()
            prefs.saveBudget(budget);       // apply()
            panelSettings.setVisibility(View.GONE);
            updateGreeting();
            refreshData();
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show();
        });

        // [Database] tambah pengeluaran
        btnAdd.setOnClickListener(v -> {
            String desc = textOf(etDesc).trim();
            long amount = parseAmount(textOf(etAmount));

            tilDesc.setError(null);
            tilAmount.setError(null);

            if (desc.isEmpty()) {
                tilDesc.setError("Isi keterangan");
                return;
            }
            if (amount <= 0) {
                tilAmount.setError("Isi jumlah lebih dari 0");
                return;
            }
            db.insert(desc, amount);
            etDesc.setText("");
            etAmount.setText("");
            etDesc.clearFocus();
            etAmount.clearFocus();
            refreshData();
        });
    }

    // [SharedPreferences] BACA pengaturan setiap layar tampil
    @Override
    protected void onResume() {
        super.onResume();

        if (panelSettings.getVisibility() == View.GONE) {
            String name = prefs.getUserName();
            if (!textOf(etName).equals(name)) {
                etName.setText(name);
            }
            long budget = prefs.getBudget();
            String budgetText = budget > 0 ? String.valueOf(budget) : "";
            if (!textOf(etBudget).equals(budgetText)) {
                etBudget.setText(budgetText);
            }
        }
        updateGreeting();
        refreshData();
    }

    private void confirmDelete(DatabaseHelper.Expense item) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus pengeluaran?")
                .setMessage(item.description + " - " + formatRupiah(item.amount))
                .setPositiveButton("Hapus", (d, w) -> {
                    db.delete(item.id);   // [Database]
                    refreshData();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateGreeting() {
        String name = prefs.getUserName();
        tvGreeting.setText(name.isEmpty() ? "Pencatat Pengeluaran" : "Halo, " + name + "!");
    }

    // Muat ulang daftar dari database + hitung ringkasan
    private void refreshData() {
        List<DatabaseHelper.Expense> expenses = db.getAll();
        adapter.setItems(expenses);
        tvEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);

        // Total dan sisa budget tidak disimpan: dihitung ulang dari database + prefs
        long total = db.getTotalSince(startOfMonth());
        long budget = prefs.getBudget();
        tvTotal.setText(formatRupiah(total));

        if (budget > 0) {
            long sisa = budget - total;
            int percent = (int) Math.min(100, total * 100 / budget);
            progressBudget.setVisibility(View.VISIBLE);
            progressBudget.setProgressCompat(percent, true);

            if (sisa >= 0) {
                progressBudget.setIndicatorColor(ContextCompat.getColor(this, R.color.progress_ok));
                tvBudgetInfo.setText("Sisa " + formatRupiah(sisa)
                        + " dari budget " + formatRupiah(budget));
            } else {
                progressBudget.setIndicatorColor(ContextCompat.getColor(this, R.color.progress_over));
                tvBudgetInfo.setText("Melebihi budget " + formatRupiah(-sisa));
            }
        } else {
            progressBudget.setVisibility(View.GONE);
            tvBudgetInfo.setText("Atur budget bulanan di menu Pengaturan");
        }
    }

    private long startOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    private long parseAmount(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static String formatRupiah(long value) {
        return "Rp " + NumberFormat.getInstance(new Locale("id", "ID")).format(value);
    }
}
