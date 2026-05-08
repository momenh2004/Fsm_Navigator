package com.fsm.navigator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fsm.navigator.R;
import com.fsm.navigator.model.PointInteret;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public interface OnNavigateClickListener {
        void onNavigateClick(PointInteret poi);
    }

    private List<PointInteret> poiList;
    private final Context context;
    private final OnNavigateClickListener listener;

    public SearchResultAdapter(Context context,
                               List<PointInteret> poiList,
                               OnNavigateClickListener listener) {
        this.context  = context;
        this.poiList  = poiList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PointInteret poi = poiList.get(position);

        holder.tvPoiName.setText(poi.getNom());

        // Détails = bâtiment + étage (remplace getDetails())
        String details = poi.getBatiment();
        if (poi.getEtage() != null && !poi.getEtage().isEmpty()) {
            details += " • " + poi.getEtage();
        }
        if (poi.isAccessiblePmr()) {
            details += " • ♿";
        }
        holder.tvPoiDetails.setText(details);

        holder.tvPoiCategory.setText(poi.getCategorie());
        holder.ivPoiIcon.setImageResource(getIconForCategory(poi.getCategorie()));

        holder.btnNavigate.setOnClickListener(v -> {
            if (listener != null) listener.onNavigateClick(poi);
        });
    }

    @Override
    public int getItemCount() {
        return poiList != null ? poiList.size() : 0;
    }

    public void updateList(List<PointInteret> newList) {
        this.poiList = newList;
        notifyDataSetChanged();
    }

    private int getIconForCategory(String categorie) {
        if (categorie == null) return android.R.drawable.ic_menu_compass;
        switch (categorie) {
            case "Amphithéâtres":  return android.R.drawable.ic_menu_agenda;
            case "Administration": return android.R.drawable.ic_menu_manage;
            case "Laboratoires":   return android.R.drawable.ic_menu_edit;
            case "Bibliothèque":   return android.R.drawable.ic_menu_info_details;
            case "Départements":   return android.R.drawable.ic_menu_myplaces;
            default:               return android.R.drawable.ic_menu_compass;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView    ivPoiIcon;
        TextView     tvPoiName, tvPoiDetails, tvPoiCategory;
        LinearLayout btnNavigate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoiIcon    = itemView.findViewById(R.id.ivPoiIcon);
            tvPoiName    = itemView.findViewById(R.id.tvPoiName);
            tvPoiDetails = itemView.findViewById(R.id.tvPoiDetails);
            tvPoiCategory= itemView.findViewById(R.id.tvPoiCategory);
            btnNavigate  = itemView.findViewById(R.id.btnNavigate);
        }
    }
}