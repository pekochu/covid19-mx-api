package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.model.covid19.Reporte;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTimeComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.Collator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CovidSummaryProviderImpl implements CovidSummaryProvider {

    @Autowired
    ReporteService reporteService;
    @Autowired
    EstadoService estadoService;

    private final static Logger LOGGER = LoggerFactory.getLogger(CovidSummaryProvider.class.getCanonicalName());

    private final static String MATHDROID_API_URL = "https://covid19.mathdro.id/api/countries/%s/%s";
    private final static String SINAVE_URL = "https://covid19.sinave.gob.mx/Log.aspx/Grafica22";
    private final static String OFFICIAL_MIRROR = "https://www.gob.mx/salud/documentos/datos-abiertos-152127";
    private final static String UNAM_MIRROR = "https://repounam.org/data/.input";
    private final static String SEMAFORO_CONACYT = "https://datos.covid-19.conacyt.mx/Semaforo/semaforo.php";

    // Datos abiertos
    // URL: https://www.gob.mx/salud/documentos/datos-abiertos-152127
    // MIRROR: https://repounam.org/data/.input/
    // Repo URL
    // http://datosabiertos.salud.gob.mx/gobmx/salud/datos_abiertos/datos_abiertos_covid19.zip
    // https://repounam.org/data/.input/2020-10-24.zip

    @Override
    public void updateData() {
        downloadDataCSV();

        // Check if the last report has the same date of today's date
        LOGGER.info("Data updated. Looking for recoveries... \n");
        getRecoveries();
    }

    @Override
    public void downloadDataCSV(){
        Date today = new Date();
        // SimpleDateFormats for date comparision
        SimpleDateFormat sdf = new SimpleDateFormat("yyMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd");
        sdfJson.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        SimpleDateFormat mxSdf = new SimpleDateFormat("d 'de' MMMM 'de' y", new Locale("es", "mx"));
        sdfJson.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        // Workplace
        Path zipFile = Paths.get("./downloads/covidmx/", "covid_open_data.zip");
        JSONObject jsonFileFlag;
        Boolean time2Download = false;
        String linkDownload;

        try{
            // Get latest update
            Reporte latest = reporteService.lastReport();
            Date latestDate = sdfJson.parse(latest.getFecha());

            // Read file in order to perform comparision
            StringBuilder jsonStringFlag = new StringBuilder();

            // Select mirror
            LOGGER.info("Checking website for updates");
            Document page = Jsoup.connect(OFFICIAL_MIRROR)
                    .userAgent("Mozilla")
                    .ignoreContentType(true).get();

            if(page.title().equalsIgnoreCase("Attack Detected")){
                // The page of gobmx has blocked us
                // Use the UNAM Mirror repostory instead, which is slower
                page = Jsoup.connect(UNAM_MIRROR)
                        .userAgent("Mozilla")
                        .ignoreContentType(true).get();
                LOGGER.info("Using the UNAM repository...");
                String md5page = DigestUtils.md5Hex(page.data().getBytes());
                Element linkZip = page.select("body > table > tbody > tr:nth-child(5) > td:nth-child(2) > a").first();
                String unamLastUpdate = linkZip.text().replace(".zip", "");
                Date unamDate = sdfJson.parse(unamLastUpdate);
                linkDownload = UNAM_MIRROR + "/" + linkZip.attr("href");
                time2Download = DateTimeComparator.getDateOnlyInstance().compare(latestDate, unamDate) < 0;
            }else{
                // Get ZIP from the official mirror
                LOGGER.info("Using the official repository");
                String md5page = DigestUtils.md5Hex(page.data().getBytes());
                Element divUpdate = page.select("body > main > div > div:nth-child(2) > div:nth-child(3) > div > dl > dd:nth-child(4)").first();
                Date officialDate = mxSdf.parse(divUpdate.text());
                Element linkZip = page.select("body > main > div > div:nth-child(2) > div:nth-child(4) > div.article-body.bottom-buffer > table:nth-child(4) > tbody > tr:nth-child(1) > td:nth-child(2) > a").first();
                String officialLastUpdate = divUpdate.text();
                time2Download = DateTimeComparator.getDateOnlyInstance().compare(latestDate, officialDate) < 0;
                linkDownload = linkZip.attr("href");
            }

            // Trigger download
            if(time2Download){
                LOGGER.info("Updating info. Downloading CSV...");
                ReadableByteChannel readChannel = Channels.newChannel(new URL(linkDownload).openStream());
                FileOutputStream fileOS = new FileOutputStream(zipFile.toFile().getAbsolutePath());
                FileChannel writeChannel = fileOS.getChannel();
                writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
                // Ready
                LOGGER.info("Zip file downloaded");
                File zipOpenData = zipFile.toFile();
                // Calculate MD5
                String checkSum = DigestUtils.md5Hex(Files.readAllBytes(zipOpenData.toPath()));

                // Unzip file
                new ZipFile(zipFile.toFile()).extractFile(String.format("%sCOVID19MEXICO.csv",
                        sdf.format(today)), "./downloads/covidmx/", "covid19.csv");
                LOGGER.info("CSV file unzipped");
                // Read the CSV
                readCovidCSV();
                // Closing streams
                fileOS.close();
                // Delete files on directory to save space on disk
                LOGGER.info("Performing deletes");
                FileUtils.cleanDirectory(new File("./downloads/covidmx/"));
            }else{
                LOGGER.info("Everything is up to date");
            }

        } catch (IOException | ParseException e) {
            LOGGER.error(e.getMessage());
        }

    }

    @Override
    public void readCovidCSV(){
        // Fecha de corte
        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fecha_corte = sdf.format(today);
        // CSV
        Path csvFile = Paths.get("./downloads/covidmx/", "covid19.csv");
        String[] cves = {
                "", "AGU", "BCN", "BCS", "CAM", "COA", "COL", "CHP", "CHH", "CMX", "DUR", "GUA", "GRO", "HID", "JAL",
                "MEX", "MIC", "MOR", "NAY", "NLE", "OAX", "PUE", "QUE", "ROO", "SLP", "SIN", "SON", "TAB", "TAM",
                "TLA", "VER", "YUC", "ZAC", "NAL" // NACIONAL
        };
        int NAL = cves.length - 2;

        if(!csvFile.toFile().exists()){
            LOGGER.error("File doesnt exist!");
        }else{
            // JSON Object
            JSONObject summary = new JSONObject();
            JSONObject estado;
            for(int i = 1; i < cves.length; i++){
                estado = new JSONObject();
                estado.put("CVE", cves[i]);
                estado.put("fecha", fecha_corte);
                estado.put("sospechosos", 0L);
                estado.put("confirmados", 0L);
                estado.put("defunciones", 0L);
                summary.put(String.valueOf(i-1), estado);
            }
            // Continue reading the csv file
            CsvParserSettings settings = new CsvParserSettings();
            settings.getFormat().setLineSeparator("\n");
            settings.setHeaderExtractionEnabled(true);
            CsvParser parser = new CsvParser(settings);

            LOGGER.info("Parse begins");
            parser.beginParsing(csvFile.toFile(), StandardCharsets.ISO_8859_1);
            Record record;
            while ((record = parser.parseNextRecord()) != null) {
                int clasificacion_final = record.getInt("CLASIFICACION_FINAL");
                String fecha_defuncion = record.getString("FECHA_DEF");
                int entidad = record.getInt("ENTIDAD_UM") - 1;

                if(clasificacion_final == 6){
                    // SOSPECHOSOS
                    summary.getJSONObject(String.valueOf(entidad)).increment("sospechosos"); // ESTADO
                    summary.getJSONObject(String.valueOf(NAL)).increment("sospechosos"); // NAL
                }else if(clasificacion_final == 1 || clasificacion_final == 2 || clasificacion_final == 3){
                    // POSITIVOS
                    summary.getJSONObject(String.valueOf(entidad)).increment("confirmados"); // ESTADO
                    summary.getJSONObject(String.valueOf(NAL)).increment("confirmados"); // NAL
                    // DEFUNCIONES
                    if(!fecha_defuncion.equals("9999-99-99")){
                        try{
                            Date defuncion = sdf.parse(fecha_defuncion);
                            if(today.compareTo(defuncion) >= 0){
                                summary.getJSONObject(String.valueOf(entidad)).increment("defunciones"); // ESTADO
                                summary.getJSONObject(String.valueOf(NAL)).increment("defunciones"); // NAL
                            }
                        } catch (ParseException e) {
                            LOGGER.error(e.getMessage());
                        }

                    }
                }
            } // end-while

            Reporte latest = reporteService.lastReport();

            List<Estado> estados = estadoService.findAll();
            Map<String, Estado> estadosHash = new HashMap<>();
            estados.forEach(state -> {
                estadosHash.put(state.getCve(), state);
            });

            Map<Integer, String> semaforoValores = getSemaphore();
            List<Reporte> reportes = new ArrayList<>();
            summary.keySet().forEach(k -> {
                JSONObject estadoJson = (JSONObject)summary.get(k);
                Reporte reporteEstatal = new Reporte(
                        estadoJson.getString("fecha"),
                        estadoJson.getLong("sospechosos"),
                        estadoJson.getLong("confirmados"),
                        estadoJson.getLong("defunciones"),
                        0L,
                        0L
                );
                reporteEstatal.setSemaforo(semaforoValores.get(
                        estadosHash.get(estadoJson.getString("CVE")).getId().intValue()
                ));
                reporteEstatal.setEstado(estadosHash.get(estadoJson.getString("CVE")));
                reportes.add(reporteEstatal);
            });
            LOGGER.info("Saving {} reports... ", reportes.size());
            reporteService.saveAllReports(reportes);
            LOGGER.info("All reports saved.");
        }
    }

    @Override
    public Map<Integer, String> getSemaphore(){
        Map<Integer, String> semaforo_epidemiologico = null;
        // Posible patterns
        // SColors\[[^\[]*\]=\'([^\']*)\';
        // Best: SColors\[([^\[]*)\]=\'([^\']*)\';

        try {
            Document page = Jsoup.connect(SEMAFORO_CONACYT)
                    .userAgent("Mozilla")
                    .ignoreContentType(true).get();

            Element scripts = page.select("script").last();
            Pattern arraySemaforo = Pattern.compile("SColors\\[([^\\[]*)\\]=\\'([^\\']*)\\';");
            Matcher m = arraySemaforo.matcher(scripts.html());

            boolean b = m.find();

            if(!b){
                return null;
            }else{
                semaforo_epidemiologico = new HashMap<>();
            }

            while(b){
                semaforo_epidemiologico.put(Integer.valueOf(m.group(1).replace("'", "")), m.group(2));
                b = m.find();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }

        return semaforo_epidemiologico;
    }

    @Override
    public void getRecoveries(){
        Collator mCollator = Collator.getInstance();
        mCollator.setStrength(Collator.NO_DECOMPOSITION);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        SimpleDateFormat dateTimeInUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        dateTimeInUTC.setTimeZone(TimeZone.getTimeZone("UTC"));

        Reporte latest = reporteService.lastReport();
        JSONArray details = null;
        try{
            details = getCovidDetails("MEX");
            Date hopkinsDate = new Date(details.getJSONObject(0).getLong("lastUpdate"));
            List<Reporte> reportesEstados = reporteService.topStateReportsByDate(latest.getFecha());

            if(sdf.format(hopkinsDate).equals(latest.getFecha())){
                LOGGER.info("Entering updating recoveries...\n");
                for(Reporte porEstado : reportesEstados){
                    Estado estado = porEstado.getEstado();
                    if(estado.getNombre().equals("Estado de México")) estado.setNombre("México");
                    for(Object hopkinsPorEstado : details){
                        JSONObject hopkinsEstado = (JSONObject) hopkinsPorEstado;
                        if(mCollator.compare(hopkinsEstado.get("provinceState"), estado.getNombre()) == 0){
                            porEstado.setRecuperados(0L);
                            long activos = porEstado.getConfirmados() -
                                    (porEstado.getDefunciones() + porEstado.getRecuperados());
                            porEstado.setActivos(activos);
                        }
                    }
                }
            }else{
                LOGGER.info("Can't update recoveries because of dates differences. {} != {}\n",
                        sdf.format(hopkinsDate), latest.getFecha());
            }

            // Save reports
            reporteService.updateReport(reportesEstados);
        }catch (Exception e){
            LOGGER.error("Error updating records. Details: {}\n", e.getMessage());
        }
    }

    @Override
    public JSONObject getCovidSummary(String country){
        JSONObject news = null;
        try{
            news = new JSONObject(Jsoup.connect(String.format(MATHDROID_API_URL, country, ""))
                    .ignoreContentType(true)
                    .get().body().text());

            LOGGER.info("ARCGIS CONSULTADA CORRECTAMENTE");
        }catch(IOException | JSONException e){
            LOGGER.error(e.getMessage());
        }

        return news;
    }

    @Override
    public JSONArray getCovidDetails(String country){
        JSONArray object = null;
        try{
            object = new JSONArray(Jsoup.connect(String.format(MATHDROID_API_URL, country, "confirmed"))
                    .ignoreContentType(true)
                    .get().body().text());

            LOGGER.info("ARCGIS CONSULTADA CORRECTAMENTE");
        }catch(IOException | JSONException e){
            LOGGER.error(e.getMessage());
        }

        return object;
    }

    @Override
    public JSONObject getCovidMX(){
        JSONObject news = null;
        String result = null;
        int i = 1;
        try{
            news = new JSONObject(Jsoup.connect(SINAVE_URL)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .followRedirects(true)
                    .method(Connection.Method.POST)
                    .header("accept", "*/*")
                    .header("accept-language", "en-US,en;q=0.9")
                    .header("content-Type", "application/json")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-origin")
                    .header("x-requested-with", "XMLHttpRequest")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/81.0.4044.113 Safari/537.36 Edg/81.0.416.58")
                    .requestBody("{}")
                    .execute().body());

            result = news.get("d").toString().replace("[[", "[").replace("]]", "]");
            news = new JSONObject();
            for(String s : result.split(",\\[")){
                String res = "[".concat(s);
                res = res.replace("[[", "[");
                news.put(String.valueOf(i++), new JSONArray(res));
            }

            LOGGER.info("SINAVE/SISVER CONSULTADA CORRECTAMENTE");
        }catch(IOException | JSONException e){
            LOGGER.error(e.getMessage(), e);
        }

        return news;
    }


    // private methods
    @NotNull
    private java.util.List<CovidMxState> orderStates(CovidMxState[] estados){
        java.util.List<CovidMxState> lista = new ArrayList<>();
        java.util.List<CovidMxState> ordenado = new ArrayList<>();
        int suma = 0;

        Collections.addAll(lista, estados);
        lista.remove(lista.size()-1);
        lista.sort(Comparator.comparingInt((CovidMxState::getConfirmados)));

        for(int i = lista.size()-1; i >= 0; i--){
            ordenado.add(lista.get(i));
        }

        return ordenado;
    }

    // inner classes
    private static class CovidMxState{

        private int id;
        private String label;
        private long habitantes;
        private int confirmados;
        private int estudiados;
        private int sospechosos;
        private int defunciones;
        private int recuperados;

        public CovidMxState(int id, String label, long habitantes, int confirmados, int estudiados, int sospechosos, int defunciones, int recuperados) {
            this.id = id;
            this.label = label;
            this.habitantes = habitantes;
            this.confirmados = confirmados;
            this.estudiados = estudiados;
            this.sospechosos = sospechosos;
            this.defunciones = defunciones;
            this.recuperados = recuperados;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public long getHabitantes() {
            return habitantes;
        }

        public void setHabitantes(long habitantes) {
            this.habitantes = habitantes;
        }

        public int getConfirmados() {
            return confirmados;
        }

        public void setConfirmados(int confirmados) {
            this.confirmados = confirmados;
        }

        public int getEstudiados() {
            return estudiados;
        }

        public void setEstudiados(int estudiados) {
            this.estudiados = estudiados;
        }

        public int getSospechosos() {
            return sospechosos;
        }

        public void setSospechosos(int sospechosos) {
            this.sospechosos = sospechosos;
        }

        public int getDefunciones() {
            return defunciones;
        }

        public void setDefunciones(int defunciones) {
            this.defunciones = defunciones;
        }

        public int getRecuperados() {
            return recuperados;
        }

        public void setRecuperados(int recuperados) {
            this.recuperados = recuperados;
        }
    }
}
