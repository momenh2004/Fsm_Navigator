package com.fsm.navigator.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fsm.navigator.R;
import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.model.PointInteret;
import com.fsm.navigator.service.FavoriService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    public interface OnNavigateClickListener {
        void onNavigateClick(PointInteret poi);
    }

    private List<PointInteret> poiList;
    private final Context context;
    private final OnNavigateClickListener listener;
    private final boolean isLoggedIn;

    // Cache local : salleId → favoriId (-1 si pas favori)
    private final Map<Long, Long> favoriCache = new HashMap<>();

    public SearchResultAdapter(Context context,
                               List<PointInteret> poiList,
                               OnNavigateClickListener listener) {
        this.context    = context;
        this.poiList    = poiList;
        this.listener   = listener;
        this.isLoggedIn = TokenManager.isLoggedIn(context);
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

        String details = poi.getBatiment();
        if (poi.getEtage() != null && !poi.getEtage().isEmpty())
            details += " • " + poi.getEtage();
        if (poi.isAccessiblePmr())
            details += " • ♿";
        holder.tvPoiDetails.setText(details);

        holder.tvPoiCategory.setText(poi.getCategorie());
        holder.ivPoiIcon.setImageResource(getIconForCategory(poi.getCategorie()));

        holder.btnNavigate.setOnClickListener(v -> {
            if (listener != null) listener.onNavigateClick(poi);
        });

        // ── Étoile favori ─────────────────────────────────────
        if (holder.btnFavori == null) return;

        if (!isLoggedIn) {
            holder.btnFavori.setVisibility(View.GONE);
            return;
        }

        holder.btnFavori.setVisibility(View.VISIBLE);
        long salleId = poi.getId();

        // État depuis cache ou API
        if (favoriCache.containsKey(salleId)) {
            updateStarUI(holder, favoriCache.get(salleId) != -1L);
        } else {
            holder.btnFavori.setEnabled(false);
            FavoriService.checkFavori(context.getApplicationContext(), "SALLE", salleId, -1,
                    new FavoriService.FavoriCallback() {
                        @Override
                        public void onSuccess(boolean isFavori, long favoriId) {
                            favoriCache.put(salleId, isFavori ? favoriId : -1L);
                            updateStarUI(holder, isFavori);
                            holder.btnFavori.setEnabled(true);
                        }
                        @Override
                        public void onError(String msg) {
                            holder.btnFavori.setEnabled(true);
                        }
                    });
        }

        // Toggle au clic
        holder.btnFavori.setOnClickListener(v -> {
            holder.btnFavori.setEnabled(false);
            boolean isFavori = favoriCache.containsKey(salleId)
                    && favoriCache.get(salleId) != -1L;

            if (isFavori) {
                long favoriId = favoriCache.get(salleId);
                FavoriService.deleteFavori(context.getApplicationContext(), favoriId,
                        new FavoriService.SimpleCallback() {
                            @Override
                            public void onSuccess(String msg) {
                                favoriCache.put(salleId, -1L);
                                updateStarUI(holder, false);
                                holder.btnFavori.setEnabled(true);
                                Toast.makeText(context.getApplicationContext(), "Retiré des favoris",
                                        Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onError(String msg) {
                                holder.btnFavori.setEnabled(true);
                                Toast.makeText(context.getApplicationContext(), "Erreur : " + msg,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                FavoriService.addFavori(context, "SALLE", poi.getNom(),
                        salleId, -1,
                        new FavoriService.SimpleCallback() {
                            @Override
                            public void onSuccess(String msg) {
                                FavoriService.checkFavori(context.getApplicationContext(), "SALLE", salleId, -1,
                                        new FavoriService.FavoriCallback() {
                                            @Override
                                            public void onSuccess(boolean is, long fid) {
                                                favoriCache.put(salleId, fid);
                                                updateStarUI(holder, true);
                                                holder.btnFavori.setEnabled(true);
                                            }
                                            @Override
                                            public void onError(String e) {
                                                holder.btnFavori.setEnabled(true);
                                            }
                                        });
                                Toast.makeText(context.getApplicationContext(), "Ajouté aux favoris",
                                        Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onError(String msg) {
                                holder.btnFavori.setEnabled(true);
                                Toast.makeText(context.getApplicationContext(), "Erreur : " + msg,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void updateStarUI(ViewHolder holder, boolean isFavori) {
        if (holder.btnFavori == null) return;
        holder.btnFavori.setImageResource(isFavori
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);
    }

    @Override
    public int getItemCount() {
        return poiList != null ? poiList.size() : 0;
    }

    public void updateList(List<PointInteret> newList) {
        this.poiList = newList;
        favoriCache.clear();
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
        ImageView    btnFavori;
        TextView     tvPoiName, tvPoiDetails, tvPoiCategory;
        LinearLayout btnNavigate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPoiIcon    = itemView.findViewById(R.id.ivPoiIcon);
            tvPoiName    = itemView.findViewById(R.id.tvPoiName);
            tvPoiDetails = itemView.findViewById(R.id.tvPoiDetails);
            tvPoiCategory= itemView.findViewById(R.id.tvPoiCategory);
            btnNavigate  = itemView.findViewById(R.id.btnNavigate);
            btnFavori    = itemView.findViewById(R.id.btnFavori);
        }
    }
}