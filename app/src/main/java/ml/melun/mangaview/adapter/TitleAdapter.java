package ml.melun.mangaview.adapter;
import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Title;

import static ml.melun.mangaview.MainApplication.p;

public class TitleAdapter extends RecyclerView.Adapter<TitleAdapter.ViewHolder> implements Filterable {

    private ArrayList<Title> mData;
    private ArrayList<Title> mDataFiltered;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mainContext;
    Boolean dark = false;
    Boolean save;
    Boolean resume = true;
    Boolean updated = false;
    boolean forceThumbnail = false;
    String path = "";
    Filter filter;

    public TitleAdapter(Context context) {
        init(context);
    }
    public TitleAdapter(Context context, Boolean online) {
        init(context);
        forceThumbnail = !online;
    }

    public void setForceThumbnail(boolean b){
        this.forceThumbnail = b;
    }
    void init(Context context){
        p = new Preference(context);
        dark = p.getDarkTheme();
        save = p.getDataSave();
        this.mInflater = LayoutInflater.from(context);
        mainContext = context;
        this.mData = new ArrayList<>();
        this.mDataFiltered = new ArrayList<>();
        filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String query = charSequence.toString();
                if(query.isEmpty() || query.length() == 0){
                    mDataFiltered = mData;
                }else{
                    ArrayList<Title> filtered = new ArrayList<>();
                    for(Title t : mData){
                        if(t.getName().toLowerCase().contains(query.toLowerCase()))
                            filtered.add(t);
                    }
                    mDataFiltered = filtered;
                }
                FilterResults res = new FilterResults();
                res.values = mDataFiltered;
                System.out.println(mData.size() +"  " +mDataFiltered.size());
                return res;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mDataFiltered = (ArrayList<Title>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    public void removeAll(){
        int originSize = mData.size();
        mData.clear();
        mDataFiltered.clear();
        notifyItemRangeRemoved(0,originSize);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_title, parent, false);
        return new ViewHolder(view);
    }

    public void addData(List<?> t){
        int oSize = mData.size();
        for(Object d:t){
            if(d instanceof Title){
                ((Title) d).setBookmark(p.getBookmark((Title)d));
                mData.add((Title)d);
            } else if(d instanceof MTitle){
                Title d2 = new Title((MTitle)d);
                d2.setBookmark(p.getBookmark((MTitle) d));
                mData.add(d2);
            }
        }
        mDataFiltered = mData;
        notifyItemRangeInserted(oSize,t.size());
    }

    public void setData(List<?> t){
        clearData();
        addData(t);
    }

    public void clearData(){
        mData.clear();
        mDataFiltered.clear();
        notifyDataSetChanged();
    }


    public void moveItemToTop(int from){
        mData.add(0, mData.get(from));
        mDataFiltered.add(0, mDataFiltered.get(from));
        mData.remove(from+1);
        mDataFiltered.remove(from+1);
        for(int i= from; i>0; i--){
            notifyItemMoved(i,i-1);
        }
    }

    public void remove(int pos){
        mData.remove(pos);
        mDataFiltered.remove(pos);
        notifyItemRemoved(pos);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Title data = mDataFiltered.get(position);
        String title = data.getName();
        String thumb = data.getThumb();
        String author = data.getAuthor();
        String tags ="";
        int bookmark = data.getBookmark();
        if(data.getTags().size()>0) {
            holder.tagContainer.setVisibility(View.VISIBLE);
            for (String s : data.getTags()) {
                tags += s + " ";
            }
            holder.tags.setText(tags);
        }else{
            holder.tagContainer.setVisibility(View.GONE);
        }
        holder.name.setText(title);
        holder.author.setText(author);

        if(data.hasCounter()){
            holder.counterContainer.setVisibility(View.VISIBLE);
            holder.recommend_c.setText(String.valueOf(data.getRecommend_c()));
            holder.battery_c.setText(String.valueOf(data.getBattery_s()));
            holder.comment_c.setText(String.valueOf(data.getComment_c()));
            holder.bookmark_c.setText(String.valueOf(data.getBookmark_c()));
        }else{
            //no counter
            holder.counterContainer.setVisibility(View.GONE);
        }

        if(thumb.length()>1 && (!save || forceThumbnail)) Glide.with(mainContext).load(thumb).into(holder.thumb);
        else holder.thumb.setImageBitmap(null);
        if(save && !forceThumbnail) holder.thumb.setVisibility(View.GONE);
        if(bookmark>0 && resume) holder.resume.setVisibility(View.VISIBLE);
        else holder.resume.setVisibility(View.GONE);

    }

    @Override
    public int getItemCount() {
        if(mDataFiltered != null)
            return mDataFiltered.size();
        return 0;
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        ImageView thumb;
        TextView author;
        TextView tags;
        TextView recommend_c, battery_c, comment_c, bookmark_c;
        Button resume;
        CardView card;

        View tagContainer;
        View counterContainer;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.Title);
            thumb = itemView.findViewById(R.id.Thumb);
            author =itemView.findViewById(R.id.TitleAuthor);
            tags = itemView.findViewById(R.id.TitleTag);
            card = itemView.findViewById(R.id.titleCard);
            resume = itemView.findViewById(R.id.epsButton);
            recommend_c = itemView.findViewById(R.id.TitleRecommend_c);
            battery_c = itemView.findViewById(R.id.TitleBattery_c);
            comment_c = itemView.findViewById(R.id.TitleComment_c);
            bookmark_c = itemView.findViewById(R.id.TitleBookmark_c);

            tagContainer = itemView.findViewById(R.id.TitleTagContainer);
            counterContainer = itemView.findViewById(R.id.TitleCounterContainer);


            if(dark){
                card.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.colorDarkBackground));
                resume.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.resumeDark));
            }
            card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onItemClick(getAdapterPosition());
                }
            });
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mClickListener.onLongClick(v, getAdapterPosition());
                    return true;
                }
            });
            resume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mClickListener.onResumeClick(getAdapterPosition(), p.getBookmark(mDataFiltered.get(getAdapterPosition())));
                }
            });


        }
    }

    public void setResume(boolean resume){
        this.resume = resume;
    }
    public Title getItem(int id) {
        return mDataFiltered.get(id);
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    public interface ItemClickListener {
        void onItemClick(int position);
        void onLongClick(View view, int position);
        void onResumeClick(int position, int id);
    }



    // filter

    @Override
    public Filter getFilter() {
        return filter;
    }
}