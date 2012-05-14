package ch.epfl.unison.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import ch.epfl.unison.AppData;
import ch.epfl.unison.LibraryService;
import ch.epfl.unison.R;
import ch.epfl.unison.api.JsonStruct;
import ch.epfl.unison.api.JsonStruct.RoomsList;
import ch.epfl.unison.api.UnisonAPI;
import ch.epfl.unison.api.UnisonAPI.Error;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class RoomsActivity extends SherlockActivity implements UnisonMenu.OnRefreshListener {

    private ListView roomsList;
    JsonStruct.Room[] rooms;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.rooms);
        this.setTitle(R.string.activity_title_rooms);

        Button b = (Button)this.findViewById(R.id.createRoomBtn);
        b.setOnClickListener(new OnCreateRoomListener());

        this.roomsList = (ListView)this.findViewById(R.id.roomsList);
        this.roomsList.setOnItemClickListener(new OnRoomSelectedListener());
    }

    @Override
    public void onResume() {
        super.onResume();
        this.onRefresh();
        this.startService(new Intent(LibraryService.ACTION_UPDATE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        return UnisonMenu.onCreateOptionsMenu(this, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return UnisonMenu.onOptionsItemSelected(this, this, item);
    }

    public void onRefresh() {
        this.repaintRefresh(true);
        UnisonAPI api = AppData.getInstance(this).getAPI();
        api.listRooms(new UnisonAPI.Handler<JsonStruct.RoomsList>() {

            public void callback(RoomsList struct) {
                RoomsActivity.this.rooms = struct.rooms;
                RoomsActivity.this.roomsList.setAdapter(new RoomsAdapter(struct));
                RoomsActivity.this.repaintRefresh(false);
                Toast.makeText(RoomsActivity.this, "Rooms loaded", Toast.LENGTH_SHORT).show();
            }

            public void onError(UnisonAPI.Error error) {
                Toast.makeText(RoomsActivity.this, error.jsonError.message, Toast.LENGTH_LONG).show();
                RoomsActivity.this.repaintRefresh(false);
            }

        });
    }

    public void repaintRefresh(boolean isRefreshing) {
        if (this.menu == null) {
            return;
        }

        MenuItem refreshItem = this.menu.findItem(R.id.menu_item_refresh);
        if (refreshItem != null) {
            if (isRefreshing) {
                LayoutInflater inflater = (LayoutInflater)getSupportActionBar()
                        .getThemedContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View refreshView = inflater.inflate(R.layout.actionbar_indeterminate_progress, null);
                refreshItem.setActionView(refreshView);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    private class RoomsAdapter extends ArrayAdapter<JsonStruct.Room> {

        public static final int ROW_LAYOUT = R.layout.rooms_row;

        public RoomsAdapter(JsonStruct.RoomsList list) {
            super(RoomsActivity.this, 0, list.rooms);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) RoomsActivity.this.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(ROW_LAYOUT, parent, false);
            }
            ((TextView) view.findViewById(R.id.roomName)).setText(this.getItem(position).name);
            ((TextView) view.findViewById(R.id.nbParticipants))
                    .setText(this.getItem(position).nbUsers + " people in this room.");
            return view;
        }
    }

    private class OnCreateRoomListener implements OnClickListener {

        public void onClick(View v) {
            AlertDialog.Builder alert = new AlertDialog.Builder(RoomsActivity.this);

            alert.setTitle("New Room");
            alert.setMessage("Pick a name for the room:");

            // Set an EditText view to get user input
            final EditText input = new EditText(RoomsActivity.this);
            alert.setView(input);

            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int whichButton) {

                    String name = input.getText().toString();
                    UnisonAPI api = AppData.getInstance(RoomsActivity.this).getAPI();
                    api.createRoom(name, new UnisonAPI.Handler<JsonStruct.RoomsList>() {

                        public void callback(RoomsList struct) {
                            RoomsActivity.this.roomsList.setAdapter(new RoomsAdapter(struct));
                        }

                        public void onError(Error error) {
                            Toast.makeText(RoomsActivity.this, "error, couldn't create room",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            alert.setNegativeButton("Cancel", null);
            alert.show();

        }
    }

    private class OnRoomSelectedListener implements OnItemClickListener {

        public void onItemClick(AdapterView<?> parent, View view, int position, long id)  {
            RoomsActivity.this.startActivity(new Intent(RoomsActivity.this, MainActivity.class)
                    .putExtra("rid", RoomsActivity.this.rooms[position].rid)
                    .putExtra("name", RoomsActivity.this.rooms[position].name));
        }
    }
}
