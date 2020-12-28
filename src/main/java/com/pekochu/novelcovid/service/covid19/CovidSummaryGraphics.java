package com.pekochu.novelcovid.service.covid19;

import com.pekochu.novelcovid.model.covid19.Estado;
import com.pekochu.novelcovid.model.covid19.Reporte;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.imageio.ImageIO;
import javax.transaction.Transactional;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Service
@Transactional
public class CovidSummaryGraphics {

    // TODO: Finish this class

    @Autowired
    ReporteService reporteService;

    @Autowired
    CovidSummaryProvider covidSummaryProvider;

    private final static Logger LOGGER = LoggerFactory.getLogger(CovidSummaryGraphics.class.getCanonicalName());

    public File createImageLatest(){
        Collator mCollator = Collator.getInstance();
        mCollator.setStrength(Collator.NO_DECOMPOSITION);
        // Image and resources
        Path resultImage = Paths.get("./src/main/resources/static/files/", "covidmx.png");
        Path canvasImage = Paths.get("./src/main/resources/static/files/twitter/", "infograph.png");
        Path fontsPath = Paths.get("./src/main/resources/static/assets/fonts/opensans/");
        // Useful things
        Date today = new Date();
        SimpleDateFormat reportesdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "MX"));
        // configurations
        nf.setMaximumFractionDigits(1);
        sdf.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        // file
        File outputImage = null;
        // cursor
        int x = 0, y = 0;
        try {
            if(resultImage.toFile().exists()){
                //noinspection ResultOfMethodCallIgnored
                resultImage.toFile().createNewFile();
            }
            outputImage = resultImage.toFile();
            // Read image and create canvas
            BufferedImage image = ImageIO.read(canvasImage.toFile());
            Graphics2D g = image.createGraphics();
            g.setPaintMode();
            int width = image.getWidth(), height = image.getHeight();

            // improve image rendering quality
            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_DITHERING,
                    RenderingHints.VALUE_DITHER_ENABLE);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                    RenderingHints.VALUE_STROKE_PURE);

            // latest summary
            Reporte latest = reporteService.lastReport();
            List<Reporte> reportesEstados = reporteService.topStateReportsByDate(latest.getFecha());
            /* ..............................................
             * Image charts for statistics visuals
             * ..............................................
             */
            // TODO: Improve chart
//            TimeSeriesCollection dataset = new TimeSeriesCollection(TimeZone.getTimeZone("America/Mexico_City"));
//            List<Reporte> reportesNacionales = reporteService.getAllNationalReports();
//            TimeSeries confirmadosSeries = new TimeSeries("Confirmados");
//
//            AtomicLong anteriordx = new AtomicLong(0);
//            for(Reporte reporte: reportesNacionales){
//                try {
//                    confirmadosSeries.add(new Day(reportesdf.parse(reporte.getFecha())),
//                            Double.valueOf(Math.abs(reporte.getConfirmados() - anteriordx.get())));
//                    anteriordx.set(reporte.getConfirmados());
//                } catch (ParseException e) {
//                    LOGGER.error(e.getMessage());
//                }
//            }
//
//            confirmadosSeries.delete(reportesNacionales.size()-2, reportesNacionales.size()-1);
//            confirmadosSeries.delete(0, 70);
//            dataset.addSeries(confirmadosSeries);
//            JFreeChart barChart = ChartFactory.createTimeSeriesChart("Casos nuevos", null,
//                    null, dataset, false, false, false);
//            barChart.setBackgroundImageAlpha(0);
//
//            barChart.draw(g, new Rectangle2D.Double(1250, 1484, 1820, 250), null, null);

            // fonts
            Font openLight = Font.createFont(Font.TRUETYPE_FONT, fontsPath.resolve("OpenSans-Light.ttf").toFile());
            Font openRegular = Font.createFont(Font.TRUETYPE_FONT, fontsPath.resolve("OpenSans-Regular.ttf").toFile());
            Font openSemibold = Font.createFont(Font.TRUETYPE_FONT, fontsPath.resolve("OpenSans-Semibold.ttf").toFile());
            Font openBold = Font.createFont(Font.TRUETYPE_FONT, fontsPath.resolve("OpenSans-Bold.ttf").toFile());
            Font selected = null;

            /* ..............................................
             * Numbers
             * ..............................................
             */
            Color darkpurple = new Color(0x404589);
            Color semaRed = new Color(0xFF0000);
            Color semaOrange = new Color(0xFF7000);
            Color semaYellow = new Color(0xFFC60F);
            Color semaGreen = new Color(0x02A247);
            Color semaGray = new Color(0xAAAAAA);

            // Semaforo datos
            Map<Integer, String> semaforoValores = covidSummaryProvider.getSemaphore();
            Color actualSemaforo = null;

            // top states summaries
            int s = 0;
            for(Reporte estados : reportesEstados){
                Estado state = estados.getEstado();
                if(state.getId() == 33) {
                    latest = estados;
                    continue;
                }
                if(s > 2) break;
                // semaforo
                if(semaforoValores != null){
                    String colorHex = semaforoValores.get(state.getId().intValue());
                    if(colorHex.equals("#FF7000")) actualSemaforo = semaOrange;
                    else if(colorHex.equals("#02A247")) actualSemaforo = semaGreen;
                    else if(colorHex.equals("#FF0000")) actualSemaforo = semaRed;
                    else if(colorHex.equals("#FFC60F")) actualSemaforo = semaYellow;
                    else actualSemaforo = semaGray;
                }else{
                    actualSemaforo = semaGray;
                }
                g.setColor(actualSemaforo);
                g.fillOval(1390, 479+(395*s), 180, 180);
                selected = openBold.deriveFont(Font.PLAIN, 38.0f);
                g.setColor(Color.WHITE);
                g.setFont(selected);
                printStrigCentered(g, "Semaforo", 180.0f, 40.0f, 1390.0f, 620f+(395*s));
                // labels
                selected = openBold.deriveFont(Font.BOLD, 80.0f);
                g.setFont(selected);
                g.setColor(Color.WHITE);
                g.drawString(state.getNombre(),1600, 510+(395*s));
                // numbers
                // 337.5
                long activos = estados.getConfirmados() - (estados.getRecuperados() + estados.getDefunciones());
                selected = openSemibold.deriveFont(Font.PLAIN, 72.0f);
                g.setFont(selected);
                printStrigCentered(g, nf.format(activos),
                        336.0f, 65.0f, 1600.0f, 550.0f+(395*s));

                printStrigCentered(g, nf.format(estados.getConfirmados()),
                        336.0f, 65.0f, 1937.5f, 550.0f+(395*s));

                printStrigCentered(g, nf.format(estados.getDefunciones()),
                        336.0f, 65.0f, 2275.0f, 550.0f+(395*s));

                printStrigCentered(g, nf.format(estados.getRecuperados()),
                        336.0f, 65.0f, 2612.5f, 550.0f+(395*s));

                selected = openBold.deriveFont(Font.BOLD, 42.0f);
                g.setFont(selected);
                printStrigCentered(g, "Activos",
                        336.0f, 65.0f, 1600.0f, 620.0f+(395*s));

                printStrigCentered(g, "Confirmados",
                        336.0f, 65.0f, 1937.5f, 620.0f+(395*s));

                printStrigCentered(g, "Defunciones",
                        336.0f, 65.0f, 2275.0f, 620.0f+(395*s));

                printStrigCentered(g, "Recuperados",
                        336.0f, 65.0f, 2612.5f, 620.0f+(395*s));
                s++;
            }

            // main summary
            selected = openBold.deriveFont(Font.BOLD, 140.0f);
            g.setFont(selected);
            g.setColor(darkpurple);

            g.drawString(nf.format(latest.getConfirmados()),110, 760);
            g.drawString(nf.format(latest.getDefunciones()),110, 1180);
            g.drawString(nf.format(latest.getRecuperados()),110, 1600);

            // FOOTER
            selected = openLight.deriveFont(Font.PLAIN, 44.0f);
            g.setFont(selected);
            g.setColor(Color.WHITE);
            g.drawString("https://github.com/pekochu/pekoservices", 40, height-40);
            // WRITE OUT IMAGE
            ImageIO.write(image, "png", outputImage);
        }catch(IOException | FontFormatException e){
            LOGGER.error(e.getMessage(), e);
        }

        return outputImage;
    }


    // TODO: what about this stuff
//    public File createImage(JSONObject json){
//        Collator mCollator = Collator.getInstance();
//        mCollator.setStrength(Collator.NO_DECOMPOSITION);
//
//        JSONObject json = getCovidSummary("MEX");
//        CovidSummaryProvider.CovidMxState[] estados = new CovidSummaryProvider.CovidMxState[33];
//
//        JSONArray details = null;
//        details = getCovidDetails("MEX");
//
//        JSONObject mx = null;
//        mx = getCovidMX();
//        for(int i = 1; i < 34; i++){
//            JSONArray perState = mx.getJSONArray(String.valueOf(i));
//            estados[i-1] = new CovidSummaryProvider.CovidMxState(perState.getInt(0), perState.getString(1), perState.getInt(2),
//                    perState.getInt(4), perState.getInt(5), perState.getInt(6), perState.getInt(7), perState.getInt(9));
//
//            for(Object o : details){
//                JSONObject jsondetail = (JSONObject) o;
//                if(mCollator.compare(estados[i-1].getLabel(), jsondetail.get("provinceState")) == 0){
//                    estados[i-1].setConfirmados(jsondetail.getInt("confirmed"));
//                    estados[i-1].setDefunciones(jsondetail.getInt("deaths"));
//                    estados[i-1].setRecuperados(jsondetail.getInt("recovered"));
//                }
//            }
//        }
//
//        SimpleDateFormat dateTimeInUTC = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//        dateTimeInUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        NumberFormat nf = NumberFormat.getInstance(new Locale("es", "MX"));
//        nf.setMaximumFractionDigits(1);
//        sdf.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
//        File outputImage = null;
//
//        int x, y;
//        try{
//            outputImage = File.createTempFile("twitter_news_covid_mx", ".png");
//
//            // Preamble
//            BufferedImage image = ImageIO.read(new File(RESOURCES_PATH.concat("images/")
//                    .concat("mxcovid.png")));
//            Graphics2D g = image.createGraphics();
//            int width = image.getWidth(),
//                    height = image.getHeight();
//
//            g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
//                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
//                    RenderingHints.VALUE_ANTIALIAS_ON);
//            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
//                    RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_DITHERING,
//                    RenderingHints.VALUE_DITHER_ENABLE);
//            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
//                    RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
//                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
//            g.setRenderingHint(RenderingHints.KEY_RENDERING,
//                    RenderingHints.VALUE_RENDER_QUALITY);
//            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
//                    RenderingHints.VALUE_STROKE_PURE);
//            Date d = dateTimeInUTC.parse(json.getString("lastUpdate"));
//
//            // Objetos de los datos
//            JSONObject confirmados = json.getJSONObject("confirmed");
//            JSONObject recuperados = json.getJSONObject("recovered");
//            JSONObject muertos = json.getJSONObject("deaths");
//            int activos = confirmados.getInt("value") - (recuperados.getInt("value") + muertos.getInt("value"));
//
//            // Fuentes
//            Font predefined = Font.createFont(Font.TRUETYPE_FONT, new File(RESOURCES_PATH.concat("fonts/")
//                    .concat("proxima_nova_bold.otf")));
//            Font derivate = null;
//
//            // CUERPO
//            // ===============================================
//            derivate = predefined.deriveFont(Font.PLAIN, 180.0f);
//            g.setFont(derivate);
//
//            // CANTIDADES
//            printStrigCentered(g, nf.format(activos),
//                    700.0f, 215.0f, 145.0f, 600.0f);
//
//            derivate = predefined.deriveFont(Font.PLAIN, 150.0f);
//            g.setFont(derivate);
//
//            printStrigCentered(g, nf.format(confirmados.getInt("value")),
//                    700.0f, 150.0f, 1022.0f, 570.0f);
//
//            printStrigCentered(g, nf.format(recuperados.getInt("value")),
//                    700.0f, 150.0f, 145.0f, 910.0f);
//
//            printStrigCentered(g, nf.format(muertos.getInt("value")),
//                    700.0f, 150.0f, 1022.0f, 910.0f);
//
//            // PORCENTAJES
//            derivate = predefined.deriveFont(Font.PLAIN, 60.0f);
//            g.setColor(Color.BLACK);
//            g.setFont(derivate);
//
//            float rate_poblacion = ((float)(confirmados.getInt("value")) / estados[32].habitantes) * 100;
//            float rate_fatality = ((float) muertos.getInt("value") /
//                    (float) confirmados.getInt("value")) * 100;
//            float rate_recovery = ((float) recuperados.getInt("value") /
//                    (float) confirmados.getInt("value")) * 100;
//
//            printStrigCentered(g, String.format("%s%% POBLACIÓN", nf.format(rate_poblacion)),
//                    680.0f, 95.0f, 1022.0f, 719.0f);
//
//            g.setColor(Color.WHITE);
//            printStrigCentered(g, String.format("%s%% RECUPERACIÓN", nf.format(rate_recovery)),
//                    680.0f, 95.0f, 155.0f, 1058.0f);
//
//            printStrigCentered(g, String.format("%s%% MORTALIDAD", nf.format(rate_fatality)),
//                    680.0f, 95.0f, 1022.0f, 1058.0f);
//
//            // ESTADOS DE LA REPUBLICA
//            // ===============================================
//            if(mx != null && details != null){
//                int s = 0;
//                java.util.List<CovidSummaryProvider.CovidMxState> ordenado = orderStates(estados);
//                derivate = predefined.deriveFont(Font.PLAIN, 55.0f);
//                g.setFont(derivate);
//
//                for(CovidSummaryProvider.CovidMxState o : ordenado){
//                    g.drawString(String.format("%d.- %S", s+1, o.getLabel()), 130, 1540+(152*s));
//
//                    // Confirmados
//                    float rate_top_poblacion = ((float)(o.getConfirmados()) / o.getHabitantes()) * 100;
//                    printStrigCentered(g, nf.format(o.getConfirmados()),
//                            320.0f, 90.0f, 800.0f, 1445.0f+(152*s));
//                    printStrigCentered(g, String.format("(%s%%)", nf.format(rate_top_poblacion)),
//                            320.0f, 90.0f, 800.0f, 1510.0f+(152*s));
//
//                    // Recuperados
//                    float rate_top_recovery = ((float) o.getRecuperados() / (float) o.getConfirmados()) * 100;
//                    printStrigCentered(g, nf.format(o.getRecuperados()),
//                            320.0f, 90.0f, 1120.0f, 1445+(152*s));
//                    printStrigCentered(g, String.format("(%s%%)", nf.format(rate_top_recovery)),
//                            320.0f, 90.0f, 1120.0f, 1510+(152*s));
//
//                    // Defunciones
//                    float rate_top_fatality = ((float) o.getDefunciones() / (float) o.getConfirmados()) * 100;
//                    printStrigCentered(g, nf.format(o.getDefunciones()),
//                            320.0f, 90.0f, 1440.0f, 1445+(152*s));
//                    printStrigCentered(g, String.format("(%s%%)", nf.format(rate_top_fatality)),
//                            320.0f, 90.0f, 1440.0f, 1510+(152*s));
//
//                    // Solo mostrar 5
//                    if(s == 4) break;
//                    s++;
//                }
//            }
//
//
//
//            // PIE DE INFOGRAFIA
//            // ===============================================
//            derivate = predefined.deriveFont(Font.PLAIN, 48.0f);
//            g.setFont(derivate);
//
//            g.drawString("FECHA Y HORA DE CORTE: ".concat(sdf.format(d)), 40, height-40);
//            ImageIO.write(image, "png", outputImage);
//
//            LOGGER.info("IMAGEN GENERADA: RUTA {}", outputImage.getAbsolutePath());
//        }catch(FontFormatException | HeadlessException | IOException | JSONException | ParseException e){
//            LOGGER.error(e.getMessage());
//        }
//
//        return outputImage;
//    }

    private void drawMultilineString(@NotNull Graphics2D g, String text, int x, int y) {
        int lineHeight = g.getFontMetrics().getHeight();
        for (String line : text.split("\n"))
            g.drawString(line, x, y += lineHeight);
    }

    public void printStrigCentered(@NotNull Graphics2D g, String text, float w, float h, float x, float y){
        FontMetrics fm = g.getFontMetrics();
        float x_text = 0.0f, y_text = 0.0f;
        x_text = ((w - fm.stringWidth(text)) / 2);
        y_text = ((h - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(text,  x + x_text, y + y_text);
    }
}
