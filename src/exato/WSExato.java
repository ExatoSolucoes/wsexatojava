package exato;

import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.math.BigInteger;
import java.util.HashMap;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import com.google.gson.Gson;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.util.zip.GZIPInputStream;

/**
 * Acesso a webservices Exato Soluções (exatosolucoes.com.br)
 * @author Lucas Junqueira <lucas@exatosolucoes.com.br>
 */
public class WSExato {
    
    /**
     * endereço de acesso aos webservices
     */
    private String url;
    
    /**
     * identificador do usuário do webservice
     */
    private String usuario;
    
    /**
     * log de requisição
     */
    private ArrayList<String> log;
    
    /**
     * formatador de data para logs
     */
    private SimpleDateFormat df;
    
    /**
     * Construtor do acesso aos webservices.
     * @param url a url de requisição
     * @param usuario o identificaor do usuário
     */
    public WSExato(String url, String usuario)
    {
        // recebendo valores
        this.url = url;
        this.usuario = usuario;
        this.log = new ArrayList<>();
        this.df = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
    }

    /**
     * Faz uma chamada a um webservice.
     * @param rota a rota do serviço
     * @param chave a chave de 32 caracteres do usuário
     * @param k o texto a ser usado na formação da variável "k" (sem a chave)
     * @param vars array associativo com as variáveis usadas na requisição ("r", "u" e "k" são adicionadas automaticamente)
     * @return texto JSON recebido do serviço
     */
    public String requisitar(String rota, String chave, String k, HashMap<String, String> vars) throws NoSuchAlgorithmException, MalformedURLException, IOException
    {
        // preparando resposta
        String resposta = "{ \"e\": -12 \"evt\": [ ] }";
        this.log.clear();
        this.adLog("início da requisição");
        
        // validando a rota
        if (!rota.contains("/")) {
           this.adLog("a rota indicada (" + rota  + ") é inválida");
           resposta = "{ \"e\": -10 \"evt\": [ ] }";
        } else {
            // seguindo a requisição
            this.adLog("rota definida como " + rota);
            
            // criando a chave
            k = chave + k;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(k.getBytes(), 0, k.length());
            k = new BigInteger(1, m.digest()).toString(16);
            this.adLog("chave de acesso definida como " + k);
            
            // repassando valores
            vars.put("u", this.usuario);
            vars.put("k", k);
            vars.remove("r");
            String varfinal = "r=" + this.codif(rota);
            for (String i : vars.keySet()) {
                varfinal += "&" + i + "=" + this.codif(vars.get(i));
            }
            
            // preparando conexão
            this.adLog("acessado " + this.url);
            HttpURLConnection httpClient = (HttpURLConnection) new URL(this.url).openConnection();
            httpClient.setRequestMethod("POST");
            httpClient.setRequestProperty("User-Agent", "Mozilla/5.0");
            httpClient.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            httpClient.setDoOutput(true);
           
            //enviando requisição
            try (DataOutputStream wr = new DataOutputStream(httpClient.getOutputStream())) {
                wr.writeBytes(varfinal);
                wr.flush();
            }
            
            // processando a resposta recebida
            int responseCode = httpClient.getResponseCode();
            this.adLog("código de resposta http " + responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // recuperar a resposta
                InputStream in = new BufferedInputStream(httpClient.getInputStream());
                BufferedReader reader = new BufferedReader( new InputStreamReader(in, "UTF-8"), 10240);
                StringBuilder builder = new StringBuilder();
                for (String linha = null; (linha = reader.readLine())!= null;) {
                    builder.append(linha);
                }
                resposta = builder.toString();
            } else {
                // erro na requisição
                resposta = "{ \"e\": -11 \"evt\": [ ] }";
            }
        }
        
        // finalizando a requisição
        this.adLog("fim da requisição");

        // retornando
        return (resposta);
    }
    
    /**
     * Recupera o log da última requisição.
     * @return o log da operação
     */
    public ArrayList<String> recLog()
    {
        return (this.log);
    }
    
    /**
     * Adiciona uma entrada ao log da requisição.
     * @param texto o texto a adicionar
     */
    private void adLog(String texto)
    {
        this.log.add((this.df.format(new Date()))+ " => " + texto);
    }
    
    /**
     * Codifica um texto para envio por requisição http.
     * @param texto o texto a codificar
     * @return o texto codificado
     */
    private String codif(String texto)
    {
        return (URLEncoder.encode(texto, StandardCharsets.UTF_8));
    }
    
    /**
     * Descompacta um evento de resposta.
     * @param evt o texto comprimido do evento
     * @return o texto descompactado
     * @throws IOException 
     */
    private String descEvt(String evt) throws IOException {
        boolean ok = true;
        byte[] compr = null;
        try {
            compr = Base64.getDecoder().decode(evt);
            ok = true;
        } catch (Exception e) {
           ok = false; 
        }
        if (ok) {
            ByteArrayInputStream bis = new ByteArrayInputStream(compr);
            GZIPInputStream gis = new GZIPInputStream(bis);
            BufferedReader br = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            gis.close();
            bis.close();
            return sb.toString();
        } else {
            return ("evento corrompido");
        }
    }
    
}

