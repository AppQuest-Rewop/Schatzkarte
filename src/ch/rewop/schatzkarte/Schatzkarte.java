package ch.rewop.schatzkarte;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MBTilesFileArchive;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.*;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

public class Schatzkarte extends Activity implements LocationListener {
	private MapView map;
	private IMapController mapController;
	private GeoPoint momPosition;
	private Marker marker;
	private static final String FILENAME = "Schatzkarte_Marker.txt";
	
	private LocationManager locationManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_schatzkarte);
		
		//MAP-------------------------------------------------------------
		map = (MapView) findViewById(R.id.map);
		map.setTileSource(TileSourceFactory.MAPQUESTOSM);
		 
		map.setMultiTouchControls(true);
		map.setBuiltInZoomControls(true);
		 
		mapController = map.getController();
		mapController.setZoom(18);
		 
		XYTileSource treasureMapTileSource = new XYTileSource("mbtiles", ResourceProxy.string.offline_mode, 1, 20, 256, ".png", "http://example.org/");
		 
		File file = new File(Environment. getExternalStorageDirectory (), "hsr.mbtiles");
		
		MapTileModuleProviderBase treasureMapModuleProvider = new MapTileFileArchiveProvider(new SimpleRegisterReceiver(this),
		treasureMapTileSource, new IArchiveFile[] { MBTilesFileArchive.getDatabaseFileArchive(file) });
		 
		MapTileProviderBase treasureMapProvider = new MapTileProviderArray(treasureMapTileSource, null,
		new MapTileModuleProviderBase[] { treasureMapModuleProvider });
		 
		TilesOverlay treasureMapTilesOverlay = new TilesOverlay(treasureMapProvider, getBaseContext());
		treasureMapTilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
		 
		//Overlay zu Karte hinzufügen
		map.getOverlays().add(treasureMapTilesOverlay);	
		
		//GPS --------------------------------------------------
		try{
			locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);			
		}catch(Exception e){
			Toast.makeText(this, "Kein GPS-Empfänger", Toast.LENGTH_LONG).show();
		}
		
        findViewById(R.id.saveButton).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				marker.addItem(momPosition, "", "");
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.schatzkarte, menu);
		return true;
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, this);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
	}
	
	@Override
	protected void onPause(){
		super.onPause();
		locationManager.removeUpdates(this);
	}

	@Override
	public void onLocationChanged(Location location) {
		momPosition = new GeoPoint(location.getLatitude(), location.getLongitude());
		//Toast.makeText(this, ""+ momPosition.getLongitudeE6() + "/" + momPosition.getLatitudeE6(), Toast.LENGTH_LONG).show();
		mapController.animateTo(momPosition);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void onStop(){
		super.onStop();
		try{
        	FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
        	ObjectOutputStream o = new ObjectOutputStream(fos);
        	
        	o.writeObject(marker.size());
        	for(int i = 0; i < marker.size(); i++){
        		o.writeObject(marker.createItem(i).getPoint());
        	}
        	
        	fos.close();
        }
        catch(Exception e){
        	Toast.makeText(this,""+ e.getMessage(), Toast.LENGTH_LONG).show();
        }
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		
		//Marker---------------------------------------------------
		Drawable symbol = getResources().getDrawable(android.R.drawable.star_big_on);
      	int markerWidth = symbol.getIntrinsicWidth();
      	int markerHeight = symbol.getIntrinsicHeight();
      	symbol.setBounds(0, markerHeight, markerWidth, 0);
              
        ResourceProxy resourceProxy = new DefaultResourceProxyImpl(getApplicationContext());
        marker = new Marker(symbol, resourceProxy);

        try{
        	FileInputStream fis = openFileInput(FILENAME);
        	ObjectInputStream o = new ObjectInputStream(fis);
        	
        	int max = (Integer) o.readObject();
        	GeoPoint point; ;
        	for(int i = 0; i < max ; i++){
        		point = (GeoPoint)o.readObject();
        		marker.addItem(point, "", "");
        	}
        	fis.close();
        }
        catch(Exception e){
        	Log.e(ACTIVITY_SERVICE, e.getMessage());
        }
        
        map.getOverlays().add(marker);
	}
}
