package com.example.smartsaver.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartsaver.R;
import com.example.smartsaver.models.Stock;

import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    private final Context context;
    private final List<Stock> stockList;
    private final OnStockClickListener listener;

    public interface OnStockClickListener {
        void onStockClick(Stock stock);
    }

    public StockAdapter(Context context, List<Stock> stockList, OnStockClickListener listener) {
        this.context = context;
        this.stockList = stockList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Stock stock = stockList.get(position);
        holder.stockName.setText(stock.code + " (" + stock.name + ")");
        holder.stockPrice.setText("$" + String.format("%.2f", stock.price));
        holder.stockChange.setText(String.format("%.2f%%", stock.changePercent));
        holder.stockChange.setTextColor(stock.changePercent >= 0 ? Color.parseColor("#4CAF50") : Color.RED);

        int logoResId = getLogoResId(stock.code);
        holder.logoImage.setImageResource(logoResId);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onStockClick(stock);
            }
        });
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    private int getLogoResId(String symbol) {
        switch (symbol.toUpperCase()) {
            case "AAPL": return R.drawable.apple;
            case "MSFT": return R.drawable.microsoft;
            case "GOOG": return R.drawable.google;
            case "AMZN": return R.drawable.amazon;
            case "TSLA": return R.drawable.tesla;
            case "NVDA": return R.drawable.nvdia;
            case "META": return R.drawable.meta;
            case "BABA": return R.drawable.alibaba;
            case "NFLX": return R.drawable.netflix;
            case "ADBE": return R.drawable.adobe;
            default: return R.drawable.logo_turtle;
        }
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView stockName, stockPrice, stockChange;
        ImageView logoImage;

        public StockViewHolder(@NonNull View view) {
            super(view);
            stockName = view.findViewById(R.id.stockName);
            stockPrice = view.findViewById(R.id.stockPrice);
            stockChange = view.findViewById(R.id.stockChange);
            logoImage = view.findViewById(R.id.logoImage);
        }
    }
}
