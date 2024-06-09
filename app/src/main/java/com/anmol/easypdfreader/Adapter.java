package com.anmol.easypdfreader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ShareCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    Activity context;
    List<File> list;

    public Adapter(Activity context, List<File> list) {
        this.context = context;
        this.list = list;
    }

    public void filterList(List<File> list) {
        this.list = list;
        this.notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.rv_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        File file = list.get(position);
        holder.name.setText(file.getName());
        holder.path.setText(file.getPath());
        holder.date.setText(getFormattedDate(file.lastModified()));
        holder.size.setText(getFormattedSize(file.length()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Viewer.class);
                intent.putExtra("name", file.getName());
                intent.putExtra("path", file.getPath());
                context.startActivity(intent);
            }
        });

        holder.option.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu(v, file);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView name, path, date, size;
        ImageView option;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.file_name);
            path = itemView.findViewById(R.id.file_path);
            option = itemView.findViewById(R.id.options_btn);
            date = itemView.findViewById(R.id.tv_date);
            size = itemView.findViewById(R.id.tv_size);

        }
    }

    public void popupMenu(View view, File file){
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        popupMenu.inflate(R.menu.option_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.popup_delete_btn) {
                    deleteFile(file);
                    return true;
                }
                else if (id == R.id.popup_share_btn){
                    shareFile(file);
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    public void deleteFile(File file) {
        boolean deleted = file.delete();
        if (deleted) {
            list.remove(file);
            notifyDataSetChanged();
        }
    }

    public void shareFile(File file){
        Intent shareIntent = ShareCompat.IntentBuilder.from(context).setType("application/pdf")
                .setStream(Uri.parse(file.getPath()))
                .setChooserTitle("Share PDF File")
                .createChooserIntent()
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(shareIntent);
    }

    private String getFormattedDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getFormattedSize(long size) {
        if (size < 1000) {
            return size + " B";
        } else if (size < 1000 * 1000) {
            return String.format("%.2f KB", size / 1000.0);
        } else if (size < 1000 * 1000 * 1000) {
            return String.format("%.2f MB", size / (1000.0 * 1000));
        } else {
            return String.format("%.2f GB", size / (1000.0 * 1000 * 1000));
        }
    }

}
