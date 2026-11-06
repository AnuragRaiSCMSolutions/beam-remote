package com.beamremote.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native Android widgets, no web view, accounts, network access, or third-party UI dependencies. */
public final class MainActivity extends Activity {
    private static final int GREEN = 0xFF176B5B, INK = 0xFF18342D, MUTED = 0xFF596B63, PAPER = 0xFFF5F4EF;
    private static final int IMPORT = 41, EXPORT = 42, LIBRARY_IMPORT = 43;
    private ProfileStore store;
    private IrTransmitter transmitter;
    private List<Profile> profiles = new ArrayList<>();
    private Profile current, trialProfile, beforeTrial;
    private LibraryCatalog.Brand trialBrand;
    private int trialCandidate;
    private String cachedLibraryKey = "";
    private LibraryProfile cachedLibraryProfile;
    private LinearLayout page;
    private TextView status;
    private String screen = "devices", pendingExport, startupError;
    private boolean preview, storageHealthy = true;
    private final ExecutorService fileWorker = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ProfileStore(this); transmitter = new IrTransmitter(getApplicationContext());
        preview = getPreferences(MODE_PRIVATE).getBoolean("preview", !transmitter.available());
        try { profiles = store.load(); }
        catch (Exception e) { storageHealthy = false; startupError = "Saved profiles could not be read. Existing data has been preserved. " + e.getMessage(); }
        for (Profile p : profiles) if (p.id.equals(store.selected())) current = p;
        if (current == null && !profiles.isEmpty()) current = profiles.get(0);
        if (state != null) { screen = state.getString("screen", "devices"); pendingExport = state.getString("export"); }
        render();
        if (startupError != null) alert("Storage problem", startupError);
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("screen", screen); out.putString("export", pendingExport);
    }
    @Override protected void onDestroy() { transmitter.close(); fileWorker.shutdown(); super.onDestroy(); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private TextView text(LinearLayout parent, String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, dp(5), 0, dp(7)); parent.addView(view); return view;
    }
    private void title(LinearLayout parent, String value, int size) { text(parent, value, size, INK).setTypeface(null, Typeface.BOLD); }
    private LinearLayout card() {
        LinearLayout card = column(); card.setPadding(dp(18), dp(14), dp(18), dp(18)); card.setBackground(background(Color.WHITE, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(12), 0, dp(4));
        page.addView(card, params); return card;
    }
    private Button button(LinearLayout parent, String label, boolean primary, Runnable action) {
        Button button = new Button(this); button.setText(label); button.setAllCaps(false); button.setTextSize(16);
        button.setTextColor(primary ? Color.WHITE : GREEN); button.setBackgroundTintList(null);
        button.setBackground(background(primary ? GREEN : 0xFFE8F1EB, 16)); button.setMinHeight(dp(56));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(dp(3), dp(5), dp(3), dp(5));
        if (parent.getOrientation() == LinearLayout.HORIZONTAL) { params.width = 0; params.weight = 1; }
        parent.addView(button, params); button.setOnClickListener(v -> action.run()); return button;
    }
    private LinearLayout row(LinearLayout parent) { LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); parent.addView(row); return row; }
    private void render() {
        LinearLayout shell = column(); shell.setBackgroundColor(PAPER);
        shell.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        page = column(); page.setPadding(dp(20), dp(16), dp(20), dp(24)); scroll.addView(page);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = row(shell); nav.setPadding(dp(8), dp(4), dp(8), dp(6));
        button(nav, "Devices", screen.equals("devices"), () -> { screen = "devices"; render(); });
        button(nav, "Remote", screen.equals("remote"), () -> { screen = "remote"; render(); });
        button(nav, "Help", screen.equals("help"), () -> { screen = "help"; render(); });
        setContentView(shell); shell.requestApplyInsets();
        text(page, "B E A M   /   REMOTE", 13, GREEN).setTypeface(null, Typeface.BOLD);
        if (screen.equals("help")) { help(); return; }
        title(page, screen.equals("devices") ? "A little more control." : current == null ? "Your remote" : current.name, 30);
        text(page, preview ? "PREVIEW MODE · No infrared will be sent" : transmitter.available() ? "IR ready · Point the top of your phone at the appliance" : "IR unavailable · Switch to Preview in Help", 14, MUTED);
        if (screen.equals("devices")) devices(); else remote();
    }
    private void devices() {
        text(page, "Your rooms. Your devices. One remote.", 16, MUTED);
        if (profiles.isEmpty()) {
            LinearLayout intro = card(); title(intro, "Meet your next remote", 23);
            text(intro, "Add a TV or air conditioner, choose a code family, then try a command. You can keep a separate profile for every room.", 16, MUTED);
        }
        for (Profile p : profiles) {
            LinearLayout c = card(); text(c, p.isAc() ? "AIR CONDITIONER" : "TELEVISION", 12, GREEN);
            title(c, p.name, 23); text(c, p.brand + " · " + p.model, 14, MUTED);
            button(c, "Open remote", true, () -> { current = p; store.select(p.id); screen = "remote"; render(); });
        }
        button(page, "+ Add device", true, this::addDevice);
        button(page, "Browse profile library", false, this::browseLibrary);
        button(page, "Import profile library", false, this::pickLibraryFile);
        button(page, "Import a profile", false, () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, IMPORT);
        });
    }
    private void remote() {
        if (current == null) { text(page, "Add a device to build your first remote.", 18, MUTED); button(page, "+ Add device", true, this::addDevice); return; }
        Profile p = current;
        text(page, p.brand + " · " + p.model, 15, MUTED);
        LinearLayout panel = card();
        if (p.isLibrary()) libraryControls(panel,p);
        else if (p.hasAcEncoder()) acControls(panel, p);
        else {
            title(panel, p.isAc() ? "Your AC scenes" : "Everyday controls", 21);
            if (p.commands.isEmpty()) text(panel, p.isAc() ? "Add complete AC signals, such as “Cool · 24°C · Auto fan” and “Off”." : "Add a command below or import a profile with codes for your model.", 16, MUTED);
            List<Map.Entry<String, Signal>> codes = new ArrayList<>(p.commands.entrySet());
            for (int i = 0; i < codes.size(); i += 2) {
                LinearLayout r = row(panel);
                for (int j = i; j < Math.min(i + 2, codes.size()); j++) {
                    Map.Entry<String, Signal> entry = codes.get(j);
                    button(r, entry.getKey(), entry.getKey().equals("Power"), () -> send(entry.getValue()));
                }
            }
        }
        if (!p.hasAcEncoder()) {
            status = text(panel, "Ready when you are. Appliance state is not known.", 14, MUTED);
            status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        LinearLayout manage = card();
        if (p == trialProfile) {
            title(manage,"Testing a candidate",20);
            text(manage,"Try one command and observe the AC. Save only after confirming the controls you need. No command is sent just by opening this screen.",14,MUTED);
            button(manage,"Save this remote",true,this::saveTrial);
            if (trialCandidate + 1 < trialBrand.profiles.size()) button(manage,"Try next candidate",false,() -> tryLibraryCandidate(p.libraryId,trialBrand,trialCandidate + 1));
            button(manage,"Choose another candidate",false,() -> chooseLibraryCandidate(p.libraryId,trialBrand));
            button(manage,"Cancel test",false,this::cancelTrial); return;
        }
        title(manage, "Make it yours", 20);
        text(manage, "Code families are candidates, not guaranteed matches. Test with the appliance in view.", 14, MUTED);
        button(manage, "Edit name, brand & model", false, this::editProfile);
        if (!p.hasAcEncoder()) button(manage, "Add or edit IR codes", false, this::chooseCommand);
        button(manage, "Export profile", false, this::exportProfile);
        button(manage, "Delete device", false, () -> new AlertDialog.Builder(this).setTitle("Delete " + p.name + "?")
            .setMessage("This removes the saved profile from this phone.").setNegativeButton("Cancel", null)
            .setPositiveButton("Delete", (d, w) -> {
                List<Profile> next = new ArrayList<>(profiles); next.remove(p);
                if (persist(next)) { profiles = next; current = profiles.isEmpty() ? null : profiles.get(0); store.select(current == null ? "" : current.id); screen = "devices"; render(); }
            }).show());
    }
    private void pickLibraryFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, LIBRARY_IMPORT);
    }
    private void browseLibrary() {
        LibraryStore libraries = new LibraryStore(this); String id = libraries.selected();
        if (id.isEmpty()) { new AlertDialog.Builder(this).setTitle("Add a profile library")
            .setMessage("Import a Beam profile-library file, then choose a brand and test candidates. Libraries work offline.")
            .setNegativeButton("Cancel", null).setPositiveButton("Choose file", (d,w) -> pickLibraryFile()).show(); return; }
        try (LibraryCatalog catalog = LibraryCatalog.open(libraries.file(id))) {
            List<LibraryCatalog.Brand> brands = catalog.brands;
            LinearLayout form = form(); EditText search = field(form,"Search brand", "");
            ListView list = new ListView(this); form.addView(list, new LinearLayout.LayoutParams(-1, dp(300)));
            List<LibraryCatalog.Brand> visible = new ArrayList<>(brands);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
            for (LibraryCatalog.Brand brand : visible) adapter.add(brand.name + " · " + brand.profiles.size() + " candidates");
            list.setAdapter(adapter);
            AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Profile library · " + brands.size() + " brands")
                .setView(form).setNegativeButton("Close", null).setNeutralButton("Import library", (d,w) -> pickLibraryFile()).create();
            search.addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
                public void onTextChanged(CharSequence s,int start,int before,int count) {
                    visible.clear(); adapter.clear(); String query = s.toString().toLowerCase(Locale.ROOT);
                    for (LibraryCatalog.Brand brand : brands) if (brand.name.toLowerCase(Locale.ROOT).contains(query)) {
                        visible.add(brand); adapter.add(brand.name + " · " + brand.profiles.size() + " candidates");
                    }
                }
                public void afterTextChanged(android.text.Editable e) {}
            });
            list.setOnItemClickListener((parent,view,position,rowId) -> {
                LibraryCatalog.Brand brand = visible.get(position); dialog.dismiss(); chooseLibraryCandidate(id,brand);
            }); dialog.show();
        } catch (Exception e) { alert("Library unavailable",e.getMessage()); }
    }
    private void chooseLibraryCandidate(String id, LibraryCatalog.Brand brand) {
        String[] labels = new String[brand.profiles.size()];
        for (int i=0;i<labels.length;i++) labels[i] = brand.profiles.get(i).name + " · " + brand.profiles.get(i).id;
        new AlertDialog.Builder(this).setTitle(brand.name + " · choose a candidate")
            .setItems(labels, (dialog,which) -> tryLibraryCandidate(id,brand,which)).setNegativeButton("Back",(d,w) -> browseLibrary()).show();
    }
    private void tryLibraryCandidate(String id, LibraryCatalog.Brand brand, int index) {
        try {
            LibraryCatalog.Entry entry = brand.profiles.get(index);
            Profile p = new Profile(); p.kind = "AC"; p.protocol = "LIBRARY"; p.brand = brand.name.substring(0,Math.min(60,brand.name.length()));
            p.model = entry.id.substring(0,Math.min(100,entry.id.length())); p.name = brand.name + " · " + entry.name;
            p.libraryId = id; p.libraryEntry = entry.entry;
            LibraryProfile source = libraryProfile(p); LibraryActions.initialize(p,source);
            if (trialProfile == null) beforeTrial = current;
            trialProfile = p; trialBrand = brand; trialCandidate = index; current = p; screen = "remote"; render();
        } catch (Exception e) { alert("Cannot open candidate",e.getMessage()); }
    }
    private LibraryProfile libraryProfile(Profile p) throws IOException {
        String key = p.libraryId + ":" + p.libraryEntry;
        if (!key.equals(cachedLibraryKey)) {
            try (LibraryCatalog catalog = LibraryCatalog.open(new LibraryStore(this).file(p.libraryId))) {
                LibraryCatalog.Entry entry = catalog.find(p.libraryEntry);
                if (entry == null) throw new IOException("This candidate is missing from its original library.");
                cachedLibraryProfile = catalog.load(entry); cachedLibraryKey = key;
            }
        }
        return cachedLibraryProfile;
    }
    private void saveTrial() {
        Profile p = trialProfile;
        if (p == null) return;
        if (profiles.size() >= 50) { alert("Profile limit","Remove a saved device before saving another remote."); return; }
        LinearLayout f = form(); EditText name = field(f,"Device name",p.name);
        AlertDialog dialog = formDialog("Save working remote",f,"Save remote");
        dialog.getButton(-1).setOnClickListener(v -> {
            String label = name.getText().toString().trim();
            if (label.isEmpty() || label.length()>60) { name.setError("Enter a name of 1–60 characters"); return; }
            p.name = label; List<Profile> next = new ArrayList<>(profiles); next.add(p);
            if (persist(next)) { profiles = next; store.select(p.id); trialProfile = null; beforeTrial = null; dialog.dismiss(); render(); }
        });
    }
    private void cancelTrial() { current = beforeTrial; trialProfile = null; beforeTrial = null; screen = "devices"; render(); }
    private void libraryControls(LinearLayout panel, Profile p) {
        try {
            LibraryProfile source = libraryProfile(p);
            panel.setBackground(background(0xFFE5E8E4,38));
            LinearLayout.LayoutParams shape = (LinearLayout.LayoutParams) panel.getLayoutParams();
            shape.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(40),dp(390));
            shape.gravity = Gravity.CENTER_HORIZONTAL; panel.setLayoutParams(shape);
            LinearLayout display = column(); display.setPadding(dp(14),dp(10),dp(14),dp(10));
            display.setBackground(background(0xFFD2DFC8,18)); panel.addView(display);
            text(display,"IMPORTED CANDIDATE · NOT LIVE STATUS",10,GREEN);
            TextView temperature = text(display,p.temperature + "°" + (source.minTemperature>45 && source.degreeType.equals("C") ? "" : source.degreeType),52,INK);
            temperature.setGravity(Gravity.CENTER); temperature.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);
            String modeDisplay = LibraryActions.modes(source).isEmpty() ? "Mode: button control" : LibraryActions.modeName(p.libraryModeCode);
            String fanDisplay = LibraryActions.fans(source).isEmpty() ? "Fan: button control" : "Fan code " + p.libraryFanCode;
            TextView settings = text(display,modeDisplay + " · " + fanDisplay,16,INK); settings.setGravity(Gravity.CENTER);
            status = text(display,"Ready · " + source.model + (preview ? " · Preview only" : ""),12,MUTED);
            status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            String on = LibraryActions.first(source,"POWER_ON","ON","POWER","ON_OFF","CAT_EYE","KEY_POWER_ON","BTN_POWER_ON","KEY_POWER","BTN_POWER");
            String off = LibraryActions.first(source,"POWER_OFF","OFF","POWER_OFF_1","POWER_OFF_2","KEY_POWER_OFF","BTN_POWER_OFF");
            LinearLayout power = row(panel);
            Button onButton = button(power,off == null ? "Power" : "Power on",true,() -> libraryTap(panel,p,source,on,() -> {})); onButton.setEnabled(on != null);
            if (off != null) button(power,"Power off",false,() -> libraryTap(panel,p,source,off,() -> {}));
            LinearLayout temps = row(panel);
            String down = LibraryActions.temperature(source,p,-1), up = LibraryActions.temperature(source,p,1);
            button(temps,"−  TEMP",false,() -> libraryTap(panel,p,source,down,() -> p.temperature--)).setEnabled(down != null);
            button(temps,"TEMP  +",false,() -> libraryTap(panel,p,source,up,() -> p.temperature++)).setEnabled(up != null);
            List<String> modes = LibraryActions.modes(source), fans = LibraryActions.fans(source);
            String nextMode = LibraryActions.next(modes,p.libraryModeCode), nextFan = LibraryActions.next(fans,p.libraryFanCode);
            String modeKey = LibraryActions.first(source,"MODE","MODES","KEY_MODE","BTN_MODE");
            if (modeKey == null) modeKey = LibraryActions.state(source,nextMode,p.temperature,p.libraryFanCode);
            String fanKey = LibraryActions.first(source,"FAN_SPEED","FAN SPEED","FAN","SPEED","KEY_FAN","BTN_FAN");
            if (fanKey == null) fanKey = LibraryActions.state(source,p.libraryModeCode,p.temperature,nextFan);
            final String modeCommand = modeKey, fanCommand = fanKey;
            LinearLayout basics = row(panel);
            button(basics,"Mode",false,() -> libraryTap(panel,p,source,modeCommand,() -> p.libraryModeCode = nextMode)).setEnabled(modeKey != null);
            button(basics,"Fan speed",false,() -> libraryTap(panel,p,source,fanCommand,() -> p.libraryFanCode = nextFan)).setEnabled(fanKey != null);
            String swing = LibraryActions.first(source,"SWING","SWING_UP_DOWN","AIR_SWEEP","LR_SWING","SWEEP","KEY_SWING","BTN_SWING");
            if (swing != null) button(panel,"Swing",false,() -> libraryTap(panel,p,source,swing,() -> {}));
            button(panel,"All commands · " + source.commands.size(),false,() -> chooseLibraryCommand(panel,p,source));
            text(panel,"Each tap sends the candidate's stored signal. Numbered candidates are code families, not confirmed model matches. Fan numbers are source codes; their physical speed depends on the AC. Missing commands are disabled.",13,MUTED);
            text(panel,"Extra functions and exact temperature/fan combinations are available in All commands. A beep alone does not confirm the requested setting. Timers and power commands can change appliance state.",12,MUTED);
        } catch (Exception e) { status = text(panel,e.getMessage(),15,MUTED); button(panel,"Import missing library",true,this::pickLibraryFile); }
    }
    private void libraryTap(LinearLayout panel, Profile p, LibraryProfile source, String key, Runnable change) {
        if (transmitter.isBusy()) { status.setText("Please wait for the previous signal."); return; }
        try {
            if (key == null) throw new IllegalArgumentException("This candidate does not contain that command.");
            Signal signal = source.signal(key); // Validate before changing requested settings.
            change.run(); if (p != trialProfile) persist(profiles);
            panel.removeAllViews(); libraryControls(panel,p); send(signal);
        } catch (Exception e) { alert("Command unavailable",e.getMessage()); }
    }
    private void chooseLibraryCommand(LinearLayout panel, Profile p, LibraryProfile source) {
        List<String> keys = new ArrayList<>(source.commands.keySet()); Collections.sort(keys);
        LinearLayout f = form(); EditText search = field(f,"Search command", ""); ListView list = new ListView(this);
        f.addView(list,new LinearLayout.LayoutParams(-1,dp(300))); List<String> visible = new ArrayList<>(keys);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>(visible)); list.setAdapter(adapter);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Commands · " + source.model).setView(f).setNegativeButton("Close",null).create();
        search.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void afterTextChanged(android.text.Editable e) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) {
                visible.clear(); adapter.clear(); String query = s.toString().toUpperCase(Locale.ROOT);
                for (String key : keys) if (key.contains(query)) { visible.add(key); adapter.add(key); }
            }
        });
        list.setOnItemClickListener((parent,view,position,id) -> {
            String key = visible.get(position); dialog.dismiss();
            libraryTap(panel,p,source,key,() -> {
                java.util.regex.Matcher match = java.util.regex.Pattern.compile("^([CHWD])([0-9]{1,3})F([0-9A])$").matcher(key);
                if (match.matches()) { int temp = Integer.parseInt(match.group(2)); if (temp >= 0 && temp <= 100) { p.libraryModeCode = match.group(1); p.temperature = temp; p.libraryFanCode = match.group(3); } }
            });
        }); dialog.show();
    }

    private void acControls(LinearLayout panel, Profile p) {
        panel.setBackground(background(0xFFE5E8E4, 38)); panel.setElevation(dp(3));
        LinearLayout.LayoutParams shellParams = (LinearLayout.LayoutParams) panel.getLayoutParams();
        shellParams.width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(40), dp(390));
        shellParams.gravity = Gravity.CENTER_HORIZONTAL; panel.setLayoutParams(shellParams);
        TextView brand = text(panel, "B E A M   ·   A C", 13, GREEN); brand.setGravity(Gravity.CENTER);
        LinearLayout lcd = column(); lcd.setPadding(dp(14), dp(10), dp(14), dp(10));
        lcd.setBackground(background(0xFFD2DFC8, 18)); panel.addView(lcd);
        TextView caption = text(lcd, "REMOTE SETTINGS · NOT LIVE STATUS", 10, MUTED); caption.setGravity(Gravity.CENTER);
        TextView temp = text(lcd, p.temperature + "°C", 58, INK);
        temp.setGravity(Gravity.CENTER); temp.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        String[] modeNames = {"Cool", "Dry", "Fan", p.isWhirlpool() ? "6th Sense" : p.isVoltas() ? "—" : "Auto", "Heat"};
        String fanName = p.fan == 5 ? "Auto" : p.fan == 4 ? "High" : p.fan == 2 ? "Medium" : p.fan == 1 ? "Low" : "Lowest";
        TextView summary = text(lcd, modeNames[p.mode] + "   ·   Fan " + fanName, 16, INK); summary.setGravity(Gravity.CENTER);
        if (p.isWhirlpool()) {
            String extras = (p.swing ? "Swing ON" : "Swing OFF") + "   ·   " + (p.sleep ? "Sleep ON" : "Sleep OFF") + "   ·   " + (p.turbo ? "Turbo ON" : "Turbo OFF");
            TextView flags = text(lcd, extras, 12, MUTED); flags.setGravity(Gravity.CENTER);
        }
        status = text(lcd, preview ? "Ready · Preview only" : "Ready · Point at the AC", 12, MUTED);
        status.setGravity(Gravity.CENTER); status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        if (p.isWhirlpool()) {
            Button power = button(panel, "Power on / off", true, () -> acTap(panel, p, () -> p.turbo = false, true));
            power.setBackground(background(0xFF9E3D39, 28));
        } else {
            LinearLayout power = row(panel);
            button(power, "Power on", true, () -> acTap(panel, p, () -> {}, true));
            button(power, "Off", false, () -> acTap(panel, p, () -> {}, false));
        }
        LinearLayout temperatures = row(panel);
        Button minus = button(temperatures, "−  TEMP", false, () -> acTap(panel, p, () -> { p.temperature--; p.acCommand = 2; p.turbo = false; }, !p.isWhirlpool()));
        Button plus = button(temperatures, "TEMP  +", false, () -> acTap(panel, p, () -> { p.temperature++; p.acCommand = 2; p.turbo = false; }, !p.isWhirlpool()));
        minus.setContentDescription("Temperature down one degree"); plus.setContentDescription("Temperature up one degree");
        boolean temperatureControl = !(p.isWhirlpool() && p.mode == 3) && !(p.isVoltas() && p.mode == 2);
        minus.setEnabled(temperatureControl && p.temperature > p.minTemperature());
        plus.setEnabled(temperatureControl && p.temperature < p.maxTemperature());
        LinearLayout basics = row(panel);
        button(basics, "Mode", false, () -> acTap(panel, p, () -> {
            int[] modes = p.isWhirlpool() || p.isVoltas() ? new int[]{0,1,2,4} : new int[]{0,1,2,3,4};
            int next = modes[0];
            for (int i=0;i<modes.length;i++) if (modes[i] == p.mode) next = modes[(i+1)%modes.length];
            if (p.isWhirlpool()) p.whirlpoolMode(next); else { p.mode = next; p.acCommand = 6; }
            if (p.isVoltas() && next == 1) p.fan = 5;
        }, !p.isWhirlpool()));
        Button fan = button(basics, "Fan speed", false, () -> acTap(panel, p, () -> {
            int[] speeds = p.isWhirlpool() || p.isVoltas() ? new int[]{5,1,2,4} : new int[]{5,0,1,2,4};
            int next = speeds[0];
            for (int i=0;i<speeds.length;i++) if (speeds[i] == p.fan) next = speeds[(i+1)%speeds.length];
            p.fan = next; p.acCommand = 17; p.turbo = false;
            if (p.isWhirlpool() && p.fan != 1) p.sleep = false;
        }, !p.isWhirlpool()));
        fan.setEnabled(!(p.isWhirlpool() && p.mode == 3) && !(p.isVoltas() && p.mode == 1));
        if (p.isWhirlpool()) {
            LinearLayout extras = row(panel);
            button(extras, "6th Sense", p.mode == 3, () -> acTap(panel, p, () -> p.whirlpoolMode(3), false));
            button(extras, p.swing ? "Swing · ON" : "Swing", p.swing, () -> acTap(panel, p, () -> { p.swing = !p.swing; p.acCommand = 7; }, false));
            LinearLayout comfort = row(panel);
            button(comfort, p.sleep ? "Sleep · ON" : "Sleep", p.sleep, () -> acTap(panel, p, () -> p.whirlpoolSleep(!p.sleep), false));
            button(comfort, p.turbo ? "Turbo · ON" : "Turbo", p.turbo, () -> acTap(panel, p, () -> p.whirlpoolTurbo(!p.turbo), false));
        }
        if (p.isVoltas()) {
            LinearLayout features = row(panel);
            button(features, p.swing ? "Swing · ON" : "Swing", p.swing, () -> {
                boolean next = !p.swing;
                voltasTap(panel, p, () -> p.swing = next, VoltasEncoder.swing(next));
            });
            button(features, p.sleep ? "Sleep · ON" : "Sleep", p.sleep, () -> {
                boolean next = !p.sleep;
                voltasTap(panel, p, () -> p.sleep = next, VoltasEncoder.sleep(next, p.temperature));
            });
            LinearLayout options = row(panel);
            button(options, "Turbo toggle", false, () -> voltasTap(panel, p, () -> {}, VoltasEncoder.turboToggle()));
            button(options, p.eco ? "Eco · ON" : "Eco", p.eco, () -> {
                boolean next = !p.eco;
                voltasTap(panel, p, () -> p.eco = next, VoltasEncoder.eco(next));
            });
            button(options, "Display toggle", false, () -> voltasTap(panel, p, () -> {}, VoltasEncoder.displayToggle()));
            text(panel, "Voltas 183V DZU2 protocol candidate · 17–30°C · 38 kHz. Your exact R32 model may use different codes. Turbo and display send toggle commands; appliance state cannot be confirmed over IR.", 12, MUTED);
        }
        text(panel, preview ? "Each tap previews one command. Nothing is transmitted." : "Each tap sends one command. Point the phone at the AC.", 13, MUTED);
        if (p.isWhirlpool() && p.mode == 3) text(panel, "Basic 6th Sense uses 23°C / Auto fan. Press Mode for manual controls.", 13, MUTED);
        if (p.isWhirlpool()) text(panel, "Extra functions are experimental. Sleep uses low fan; Turbo uses high fan and a temperature limit. Timers are not included. Heat requires a heating-capable AC.", 12, MUTED);
    }
    private void acTap(LinearLayout panel, Profile p, Runnable change, boolean powerAction) {
        if (transmitter.isBusy()) { status.setText("Please wait for the previous signal."); return; }
        change.run(); persist(profiles);
        panel.removeAllViews(); acControls(panel, p);
        send(p.acSignal(powerAction));
    }
    private void voltasTap(LinearLayout panel, Profile p, Runnable change, Signal signal) {
        if (transmitter.isBusy()) { status.setText("Please wait for the previous signal."); return; }
        change.run(); persist(profiles);
        panel.removeAllViews(); acControls(panel, p);
        send(signal);
    }
    private interface Selection { void choose(int index); }
    private AdapterView.OnItemSelectedListener listener(Selection selected) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { selected.choose(position); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
    }
    private void send(Signal signal) {
        TextView target = status;
        target.setText(preview ? "Preparing preview…" : "Sending…");
        transmitter.send(signal, preview, (sent, message) -> target.setText(message));
    }
    private void help() {
        title(page, "A clear connection.", 30);
        LinearLayout c = card(); title(c, "This phone", 22); text(c, transmitter.capabilities(), 16, MUTED);
        Switch toggle = new Switch(this); toggle.setText("Preview mode"); toggle.setTextSize(18); toggle.setMinHeight(dp(56)); toggle.setChecked(preview); c.addView(toggle);
        toggle.setOnCheckedChangeListener((button, checked) -> { preview = checked; getPreferences(MODE_PRIVATE).edit().putBoolean("preview", checked).apply(); });
        text(c, "Preview validates and describes a signal without transmitting it. It never changes the appliance.", 15, MUTED);
        LinearLayout tips = card(); title(tips, "Start here", 22);
        text(tips, "1. Add a device and pick its remote code family. A remote model is often printed inside its battery cover.\n\n2. Point your phone’s IR window at the appliance. Start close, with clear line of sight.\n\n3. Try one command. If it does not work, select a different profile or import verified codes.\n\n4. Rename a working profile for its room.", 16, MUTED);
        LinearLayout limits = card(); title(limits, "What to expect", 22);
        text(limits, "A built-in IR emitter must be exposed through Android’s ConsumerIrManager. Some phones require unsupported vendor APIs. An emulator cannot emit IR.\n\nIR is one-way: “sent” does not mean the appliance responded. Power toggles may become out of sync.\n\nAC protocols vary by model. LG presets cover basic power, temperature, mode and fan. Whirlpool presets include basic settings, a power toggle, and experimental basic 6th Sense, vertical swing, sleep and Turbo for DG11J1 remote families. Voltas includes a 183V DZU2 protocol candidate with basic settings and documented extras; R32 alone does not confirm compatibility. For other models or unsupported functions, import complete raw state scenes.\n\nYour phone cannot learn a remote through this API. Capturing codes requires a separate IR receiver. Bluetooth, RF and Wi-Fi remotes are not supported.\n\nProfiles stay on this phone. No account, network permission, or analytics.", 16, MUTED);
    }
    private LinearLayout form() { LinearLayout form = column(); form.setPadding(dp(22), dp(8), dp(22), dp(12)); return form; }
    private EditText field(LinearLayout form, String label, String initial) {
        text(form, label, 14, MUTED); EditText input = new EditText(this); input.setText(initial); input.setSingleLine(true); input.setMinHeight(dp(48));
        input.setContentDescription(label); form.addView(input); return input;
    }
    private Spinner spinner(LinearLayout parent, String[] choices) {
        Spinner spinner = new Spinner(this); ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, choices);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter); spinner.setMinimumHeight(dp(52)); parent.addView(spinner); return spinner;
    }
    private AlertDialog formDialog(String title, LinearLayout form, String positive) {
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(title).setView(scroll).setNegativeButton("Cancel", null).setPositiveButton(positive, null).create();
        dialog.show(); return dialog;
    }
    private void addDevice() {
        LinearLayout f = form(); EditText name = field(f, "Device name", "Living room");
        text(f, "Brand / remote code family", 14, MUTED); Spinner preset = spinner(f, Catalog.OPTIONS);
        preset.setContentDescription("Brand and remote code family");
        text(f, "Choose the closest candidate. Other brands can use imported or manually entered codes.", 14, MUTED);
        AlertDialog d = formDialog("Add a device", f, "Add device");
        d.getButton(-1).setOnClickListener(v -> {
            String label = name.getText().toString().trim();
            if (label.isEmpty() || label.length() > 60) { name.setError("Enter a name of 1–60 characters"); return; }
            if (profiles.size() >= 50) { alert("Profile limit", "Export or remove a device before adding more than 50 profiles."); return; }
            Profile p = Catalog.create(preset.getSelectedItemPosition(), label);
            List<Profile> next = new ArrayList<>(profiles); next.add(p);
            if (persist(next)) { profiles = next; current = p; store.select(p.id); screen = "remote"; d.dismiss(); render(); }
        });
    }
    private void editProfile() {
        Profile p = current; LinearLayout f = form(); EditText name = field(f, "Name", p.name);
        EditText brand = field(f, "Brand", p.brand); EditText model = field(f, "Model / remote model", p.model);
        text(f, "These labels do not change the signal protocol. Add a new device to use another code family.", 14, MUTED);
        AlertDialog d = formDialog("Device details", f, "Save");
        d.getButton(-1).setOnClickListener(v -> {
            try {
                JSONObject data = p.json().put("name", name.getText().toString().trim()).put("brand", brand.getText().toString().trim()).put("model", model.getText().toString().trim());
                Profile updated = Profile.parse(data); List<Profile> next = new ArrayList<>(profiles); next.set(profiles.indexOf(p), updated);
                if (persist(next)) { profiles = next; current = updated; d.dismiss(); render(); }
            } catch (Exception e) { alert("Check the details", e.getMessage()); }
        });
    }
    private void chooseCommand() {
        List<String> keys = new ArrayList<>(current.commands.keySet()); keys.add(0, "+ New command / scene");
        new AlertDialog.Builder(this).setTitle("IR codes").setItems(keys.toArray(new String[0]), (dialog, which) -> editCommand(which == 0 ? null : keys.get(which))).show();
    }
    private void editCommand(String key) {
        Profile p = current; Signal old = key == null ? null : p.commands.get(key);
        LinearLayout f = form(); EditText name = field(f, "Button label", key == null ? "" : key);
        EditText frequency = field(f, "Carrier frequency (Hz)", String.valueOf(old == null ? 38000 : old.frequency)); frequency.setInputType(InputType.TYPE_CLASS_NUMBER);
        StringBuilder joiner = new StringBuilder();
        if (old != null) for (int value : old.pattern()) { if (joiner.length() > 0) joiner.append(", "); joiner.append(value); }
        EditText raw = field(f, "Alternating mark / space durations (µs)", joiner.toString()); raw.setSingleLine(false); raw.setMinLines(4); raw.setMaxLines(8); raw.setGravity(Gravity.TOP);
        text(f, "Start with a mark. Paste positive decimal microseconds, separated by commas or spaces. Omit the receiver’s leading idle gap. Include any required repeats and gaps in this pattern. No hex or Pronto format.", 14, MUTED);
        AlertDialog d = formDialog("Edit IR command", f, "Save code");
        d.getButton(-1).setOnClickListener(v -> {
            try {
                String label = name.getText().toString().trim();
                if (label.isEmpty() || label.length() > 60) throw new IllegalArgumentException("Use a label of 1–60 characters.");
                if (!label.equals(key) && p.commands.containsKey(label)) throw new IllegalArgumentException("That button name already exists. Edit it from the list.");
                Profile updated = Profile.parse(p.json());
                if (key != null) updated.commands.remove(key);
                if (updated.commands.size() >= 64) throw new IllegalArgumentException("Maximum 64 commands per profile.");
                updated.commands.put(label, Signal.parse(Integer.parseInt(frequency.getText().toString()), raw.getText().toString()));
                List<Profile> next = new ArrayList<>(profiles); next.set(profiles.indexOf(p), updated);
                if (persist(next)) { profiles = next; current = updated; d.dismiss(); render(); }
            } catch (Exception e) { alert("Check the code", e.getMessage()); }
        });
        if (key != null) button(f, "Remove this command", false, () -> {
            try {
                Profile updated = Profile.parse(p.json()); updated.commands.remove(key);
                List<Profile> next = new ArrayList<>(profiles); next.set(profiles.indexOf(p), updated);
                if (persist(next)) { profiles = next; current = updated; d.dismiss(); render(); }
            } catch (Exception e) { alert("Could not remove code", e.getMessage()); }
        });
    }
    private boolean persist(List<Profile> next) {
        if (!storageHealthy) { alert("Storage unavailable", "Existing unreadable data has been preserved. Restore a backup or clear app storage in Android Settings to start again."); return false; }
        try { store.save(next); return true; } catch (RuntimeException e) { alert("Could not save", e.getMessage()); return false; }
    }
    private void exportProfile() {
        try {
            pendingExport = current.id;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "beam-profile.json");
            startActivityForResult(intent, EXPORT);
        } catch (Exception e) { alert("Could not export", e.getMessage()); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        if (request != IMPORT && request != EXPORT && request != LIBRARY_IMPORT) return;
        final android.net.Uri uri = data.getData();
        if (request == LIBRARY_IMPORT) {
            Toast.makeText(this,"Importing library…",Toast.LENGTH_SHORT).show();
            fileWorker.execute(() -> {
                try (InputStream input = getContentResolver().openInputStream(uri)) {
                    if (input == null) throw new IOException("Cannot read selected library.");
                    new LibraryStore(getApplicationContext()).install(input);
                    runOnUiThread(() -> { if (!isDestroyed()) { cachedLibraryKey = ""; browseLibrary(); } });
                } catch (Exception e) { runOnUiThread(() -> { if (!isDestroyed()) alert("Could not import library",e.getMessage()); }); }
            }); return;
        }
        String snapshot = null;
        try {
            if (request == EXPORT) {
                for (Profile p : profiles) if (p.id.equals(pendingExport)) snapshot = p.json().toString(2);
                if (snapshot == null) throw new IllegalStateException("Please export the profile again.");
                if (snapshot.getBytes(StandardCharsets.UTF_8).length > 524288)
                    throw new IllegalArgumentException("Profile exceeds the 512 KB transfer limit. Split it into smaller profiles.");
            }
        } catch (Exception e) { alert("Could not export", e.getMessage()); return; }
        final String exportData = snapshot;
        Toast.makeText(this, request == IMPORT ? "Reading profile…" : "Exporting profile…", Toast.LENGTH_SHORT).show();
        fileWorker.execute(() -> {
          try {
            if (request == EXPORT) {
                try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
                    if (out == null) throw new IOException("Cannot open destination.");
                    out.write(exportData.getBytes(StandardCharsets.UTF_8));
                }
                runOnUiThread(() -> { if (!isDestroyed()) { pendingExport = null; Toast.makeText(this, "Profile exported", Toast.LENGTH_SHORT).show(); } });
            } else if (request == IMPORT) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new IOException("Cannot open profile.");
                    byte[] block = new byte[4096]; int read;
                    while ((read = in.read(block)) != -1) {
                        if (buffer.size() + read > 524288) throw new IllegalArgumentException("Profile must be smaller than 512 KB.");
                        buffer.write(block, 0, read);
                    }
                }
                Profile imported = Profile.parse(new JSONObject(buffer.toString("UTF-8"))); imported.id = UUID.randomUUID().toString();
                runOnUiThread(() -> {
                    if (isDestroyed()) return;
                    if (profiles.size() >= 50) { alert("Profile limit", "Maximum 50 profiles. Remove a device first."); return; }
                    List<Profile> next = new ArrayList<>(profiles); next.add(imported);
                    if (persist(next)) { profiles = next; current = imported; store.select(imported.id); screen = "remote"; render(); }
                });
            }
          } catch (Exception e) { runOnUiThread(() -> { if (!isDestroyed()) alert("Could not read or write profile", e.getMessage()); }); }
        });
    }
    private void alert(String title, String message) { new AlertDialog.Builder(this).setTitle(title).setMessage(message == null ? "Please check your input and try again." : message).setPositiveButton("OK", null).show(); }
}
