package com.culinars.culinars.adapter.main;

import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.culinars.culinars.R;
import com.culinars.culinars.data.FB;
import com.culinars.culinars.data.structure.Content;
import com.culinars.culinars.data.structure.Recipe;
import com.culinars.culinars.data.structure.User;
import com.google.firebase.database.DataSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RecommendationsAdapter extends RecipeAdapter {

    int resultCount;
    public List<Recipe> data;
    public Map<String, Content> contents;
    public Map<String, Bitmap> images;

    public RecommendationsAdapter() {
        this(10);
    }

    public RecommendationsAdapter(int initResultCount) {
        resultCount = initResultCount;
        contents = new HashMap<>();
        images = new HashMap<>();
        refreshData();
    }

    @Override
    public Recipe getDataAtPos(int position) {
        if (data != null)
            return data.get(position);
        else
            return null;
    }

    @Override
    public Content getContentAtPos(int position) {
        try {
            return contents.get(getDataAtPos(position).uid);
        } catch (NullPointerException e) {
            Log.w("NULLL", ":(", e);
            return null;
        }
    }

    public int getPositionOfRecipe(Recipe recipe) {
        return data.indexOf(recipe);
    }


    @Override
    public void onFavoriteClick(final int position, final ImageView cardFavorite) {
        User.current().onGet(new FB.GetListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                User res = User.from(s);
                if (res != null) {

                    res.setFavorite(getDataAtPos(position).uid, !res.isFavorite(getDataAtPos(position).uid));
                    cardFavorite.setImageResource(res.isFavorite(getDataAtPos(position).uid) ? R.drawable.heart : R.drawable.heart_outline);
                }
            }
        });

/*        DataManager.getInstance().getUser().addOnDataReadyListener(new Reference.OnDataReadyListener<User>() {
            @Override
            public void onDataReady(User value) {
                if (value.favorites != null && value.favorites.containsKey(getDataAtPos(position).uid)) {
                    DataManager.getInstance().setFavorite(getDataAtPos(position).uid, false);
                    cardFavorite.setImageResource(R.drawable.heart_outline);
                } else {
                    DataManager.getInstance().setFavorite(getDataAtPos(position).uid, true);
                    cardFavorite.setImageResource(R.drawable.heart);
                }
            }
        });*/
    }


    public void refreshData() {
        User.current().onGet(new FB.GetListener() {
            @Override
            public void onDataChange(DataSnapshot s) {
                User res = User.from(s);
                Log.d("RecAdapter", "recommendations: " + (res==null?"user_is_null":res.recommendations));
                if (res != null && res.recommendations != null) {
                    res.getRecommendations(0, resultCount).getOnce().onComplete(getListener());
                }
            }
        });
    }

    public FB.CompleteListener getListener() {
        return new FB.CompleteListener() {
            @Override
            public void onComplete(List<DataSnapshot> results) {
                if (results.size() > 0) {
                    if (results.get(0).getKey().equals("recipes")
                            || results.get(0).getKey().equals("recommendations")
                            || results.get(0).getKey().equals("suggestions")) {
                        Iterable<DataSnapshot> children = results.get(0).getChildren();
                        results.clear();
                        for (DataSnapshot s : children)
                            results.add(s);
                    }
                    data = new ArrayList<>();
                    for (DataSnapshot d : results) {
                        Log.d("RecAdapter iteration", "adding id: " + d.getKey());
                        final Recipe r = Recipe.from(d);
                        data.add(r);
                        if (r.content != null && r.content.size() > 0) {
                            Content.load(r.content.keySet().iterator().next()).getOnce().onGet(new FB.GetListener() {
                                @Override
                                public void onDataChange(DataSnapshot s) {
                                    Content res = Content.from(s);
                                    contents.put(r.uid, res);
                                }
                            });
                        }
                    }
                    notifyDataSetChanged();
                }
            }
        };
    }

    @Override
    public int getItemCount() {
        if (data != null)
            return data.size()+1;
        else
            return 1;
    }
}
