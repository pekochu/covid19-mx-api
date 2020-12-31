package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.model.covid19.Reporte;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
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
        // Download and read CSV
        downloadDataCSV(false);
        // Read the CSV
        readCovidCSV();
    }

    @Override
    public void downloadDataCSV(boolean force){
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
        boolean time2Download = false;
        String linkDownload;

        try{
            // Get latest update
            Reporte latest = reporteService.lastReport();
            Date latestDate = latest == null ? sdfJson.parse("2020-02-01") : sdfJson.parse(latest.getFecha());

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
            if(time2Download | force){
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
                ZipFile zippedCsv = new ZipFile(zipFile.toFile());
                List<FileHeader> listFiles = zippedCsv.getFileHeaders();
                zippedCsv.extractFile(listFiles.get(0), "./downloads/covidmx/", "covid19.csv");
                LOGGER.info("CSV file unzipped");
                // Closing streams
                fileOS.close();
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
                estado.put("negativos", 0L);
                estado.put("defunciones", 0L);
                estado.put("recuperados", 0L);
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
                }else if(clasificacion_final == 7){
                    // NEGATIVOS
                    summary.getJSONObject(String.valueOf(entidad)).increment("negativos"); // ESTADO
                    summary.getJSONObject(String.valueOf(NAL)).increment("negativos"); // NAL
                }
            } // end-while

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
                        estadoJson.getLong("negativos"),
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

        try {
            // Delete files on directory to save space on disk
            LOGGER.info("Performing deletes");
            FileUtils.cleanDirectory(new File("./downloads/covidmx/"));
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void resetDataset(){
        // Download the CSV
        downloadDataCSV(true);
        // Fecha de corte
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // CSV
        Path csvFile = Paths.get("./downloads/covidmx/", "covid19.csv");
        String[] cves = {
                "", "AGU", "BCN", "BCS", "CAM", "COA", "COL", "CHP", "CHH", "CMX", "DUR", "GUA", "GRO", "HID", "JAL",
                "MEX", "MIC", "MOR", "NAY", "NLE", "OAX", "PUE", "QUE", "ROO", "SLP", "SIN", "SON", "TAB", "TAM",
                "TLA", "VER", "YUC", "ZAC", "NAL" // NACIONAL
        };
        int NAL = cves.length - 2;

        try {
            if(!csvFile.toFile().exists()){
                LOGGER.error("File doesnt exist!");
            }else{
                // Dates
                Date today = new Date();
                Date changingDate = sdf.parse("2020-02-01");
                Calendar c = Calendar.getInstance();
                c.setTime(changingDate);
                // JSON Object
                JSONObject summary = new JSONObject();
                JSONObject estado;

                // Continue reading the csv file
                CsvParserSettings settings = new CsvParserSettings();
                settings.getFormat().setLineSeparator("\n");
                settings.setHeaderExtractionEnabled(true);
                CsvParser parser = new CsvParser(settings);

                while(DateTimeComparator.getDateOnlyInstance().compare(today, changingDate) != 0){
                    LOGGER.info("Parse begins");
                    parser.beginParsing(csvFile.toFile(), StandardCharsets.ISO_8859_1);
                    Record record;
                    // initialize object
                    for(int i = 1; i < cves.length; i++){
                        estado = new JSONObject();
                        estado.put("CVE", cves[i]);
                        estado.put("fecha", sdf.format(changingDate));
                        estado.put("sospechosos", 0L);
                        estado.put("confirmados", 0L);
                        estado.put("negativos", 0L);
                        estado.put("defunciones", 0L);
                        summary.put(String.valueOf(i-1), estado);
                    }

                    // read thru csv
                    while ((record = parser.parseNextRecord()) != null) {
                        int clasificacion_final = record.getInt("CLASIFICACION_FINAL");
                        String fecha_ingreso = record.getString("FECHA_INGRESO");
                        Date dateIngreso = sdf.parse(fecha_ingreso);
                        String fecha_defuncion = record.getString("FECHA_DEF");
                        int entidad = record.getInt("ENTIDAD_UM") - 1;

                        if (DateTimeComparator.getDateOnlyInstance().compare(dateIngreso, changingDate) <= 0) {
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
                                        if(changingDate.compareTo(defuncion) >= 0){
                                            summary.getJSONObject(String.valueOf(entidad)).increment("defunciones"); // ESTADO
                                            summary.getJSONObject(String.valueOf(NAL)).increment("defunciones"); // NAL
                                        }
                                    } catch (ParseException e) {
                                        LOGGER.error(e.getMessage());
                                    }

                                }
                            }else if(clasificacion_final == 7){
                                // NEGATIVOS
                                summary.getJSONObject(String.valueOf(entidad)).increment("negativos"); // ESTADO
                                summary.getJSONObject(String.valueOf(NAL)).increment("negativos"); // NAL
                            }
                        }


                    } // end-while

                    List<Estado> estados = estadoService.findAll();
                    Map<String, Estado> estadosHash = new HashMap<>();
                    estados.forEach(state -> {
                        estadosHash.put(state.getCve(), state);
                    });

                    List<Reporte> reportes = new ArrayList<>();
                    summary.keySet().forEach(k -> {
                        JSONObject estadoJson = (JSONObject)summary.get(k);
                        Reporte reporteEstatal = new Reporte(
                                estadoJson.getString("fecha"),
                                estadoJson.getLong("sospechosos"),
                                estadoJson.getLong("confirmados"),
                                estadoJson.getLong("negativos"),
                                estadoJson.getLong("defunciones"),
                                0L,
                                0L
                        );
                        reporteEstatal.setEstado(estadosHash.get(estadoJson.getString("CVE")));
                        reportes.add(reporteEstatal);
                    });
                    LOGGER.info("Saving {} reports... ", reportes.size());
                    reporteService.saveAllReports(reportes);

                    c.add(Calendar.DATE, 1);
                    changingDate = c.getTime();
                }

                LOGGER.info("All reports saved.");
            }

            // Delete files on directory to save space on disk
            LOGGER.info("Performing deletes");
            FileUtils.cleanDirectory(new File("./downloads/covidmx/"));
        } catch (IOException | ParseException e) {
            LOGGER.error(e.getMessage(), e);
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
            details = new JSONArray("{}");
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
}
