package com.example.pencatatpengeluaran;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends BaseAdapter {

    public interface OnDeleteListener {
        void onDelete(DatabaseHelper.Expense expense);
    }

    private final Context context;
    private final OnDeleteListener deleteListener;
    private List<DatabaseHelper.Expense> items = new ArrayList<>();

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy, HH:mm", new Locale("id", "ID"));

    public ExpenseAdapter(Context context, OnDeleteListener deleteListener) {
        this.context = context;
        this.deleteListener = deleteListener;
    }

    public void setItems(List<DatabaseHelper.Expense> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public DatabaseHelper.Expense getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_expense, parent, false);
        }

        DatabaseHelper.Expense e = items.get(position);

        TextView tvAvatar = view.findViewById(R.id.tvAvatar);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView tvDate = view.findViewById(R.id.tvDate);
        TextView tvAmount = view.findViewById(R.id.tvAmount);
        ImageButton btnDelete = view.findViewById(R.id.btnDelete);

        String first = e.description.isEmpty() ? "?" : e.description.substring(0, 1).toUpperCase();
        tvAvatar.setText(first);
        tvDesc.setText(e.description);
        tvDate.setText(dateFormat.format(new Date(e.createdAt)));
        tvAmount.setText("-" + MainActivity.formatRupiah(e.amount));
        btnDelete.setOnClickListener(v -> deleteListener.onDelete(e));

        return view;
    }
}
