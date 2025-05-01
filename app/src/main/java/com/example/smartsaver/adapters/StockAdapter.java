package com.example.smartsaver.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.smartsaver.R;
import com.example.smartsaver.models.Stock;

import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {

    public interface OnStockClickListener {
        void onStockClick(Stock stock);
    }

    private final Context context;
    private final List<Stock> stocks;
    private final OnStockClickListener listener;

    public StockAdapter(Context context, List<Stock> stocks, OnStockClickListener listener) {
        this.context = context;
        this.stocks = stocks;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stockName, stockPrice, stockChange;

        public ViewHolder(View view) {
            super(view);
            stockName = view.findViewById(R.id.stockName);
            stockPrice = view.findViewById(R.id.stockPrice);
            stockChange = view.findViewById(R.id.stockChange);
        }
    }

    @Override
    public StockAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(StockAdapter.ViewHolder holder, int position) {
        Stock stock = stocks.get(position);
        holder.stockName.setText(stock.name + " (" + stock.code + ")");
        holder.stockPrice.setText("₺" + String.format("%.2f", stock.price));
        holder.stockChange.setText(String.format("%.2f", stock.changePercent) + "%");

        holder.itemView.setOnClickListener(v -> listener.onStockClick(stock));
    }

    @Override
    public int getItemCount() {
        return stocks.size();
    }
}
