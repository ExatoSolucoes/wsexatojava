package conciliadorjava;

import com.google.gson.Gson;
import exato.WSExato;
import exato.WsResposta;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Exemplo do uso do conciliador de cartões da Exato Soluções <exatosolucoes.com.br>
 * @author Lucas Junqueira <lucas@exatosolucoes.com.br>
 */
public class ConciliadorJava {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        
        // informações de acesso ao serviço (fornecidas pela Exato Soluções)
        String URLWS = "url do serviço";
        String USWS = "nome de usuário";
        String CHWS = "chave do usuário";
        String CLCONC = "identificador do cliente";
        
        // preparado webservice
        WSExato ws = new WSExato(URLWS, USWS);
        String resp = "";
        HashMap<String, String> vars;
        vars = new HashMap<>();
        
        // indique o serviço a consultar
        String servico = "";
        servico = "conciliação erp"; // conciliar informações com o sistema de vendas
        // servico = "extrato de movimentação"; // recuperar extrato de movimentação
        // servico = "conciliação bancária"; // conciliar informações de extrato bancário
        // servico = "extrato bancário"; // envio de extrato bancário
        // servico = "quitação"; // conferência de quitação do pagamento de parcelas
        
        // conciliar informações com o sistema de vendas
        if (servico.equals("conciliação erp")) {
            
            // carregando o texto do extrato bancário
            String requisicao = ConciliadorJava.textoArquivo("exemploerp.json");
            
            // variáveis de requisição
            vars.put("c", CLCONC); // identificador do cliente fornecido pela Exato Soluções
            vars.put("req", requisicao); // texto da requisição
            vars.put("t", "venda"); // tipo de requisição ("venda" ou "pagamento")
            
            // variável opcional para o retorno já descompactado
            vars.put("fr", "txt");
            
            // chave da requisição (a inclusão da chave e o cálculo do MD5 são feitos automaticamente na chamada da requisição)
            String k = USWS + vars.get("c") + vars.get("t") + vars.get("req");
            
            // requisitando as informações
            resp = ws.requisitar("vdk-cartoes/conciliacao-erp", CHWS, k, vars);
        }
        
        // recuperar extrato de movimentação
        if (servico.equals("extrato de movimentação")) {
            
            // variáveis de requisição
            vars.put("id", CLCONC); // identificador do cliente fornecido pela Exato Soluções
            vars.put("dini", "01/03/2022"); // data inicial do período
            vars.put("dfim", "05/03/2022"); // data final do período
            vars.put("tp", "venda"); // tipo de movimentação ("venda" ou "pagamento")
            vars.put("l", "exato.json"); // formato da lista a receber
            
            // variável opcional para o retorno já descompactado
            vars.put("fr", "txt");
            
            // chave da requisição (a inclusão da chave e o cálculo do MD5 são feitos automaticamente na chamada da requisição)
            String k = vars.get("id") + vars.get("dini") + vars.get("dfim") + vars.get("tp");
            
            // requisitando as informações
            resp = ws.requisitar("vdk-cartoes/extrato-recuperacao", CHWS, k, vars);
        }
        
        // conciliar informações de extrato bancário
        if (servico.equals("conciliação bancária")) {
            
            // carregando o texto do extrato bancário
            String extrato = ConciliadorJava.textoArquivo("caminho para arquivo cnab ou ofx");
            
            // variáveis de requisição
            vars.put("c", CLCONC); // identificador do cliente fornecido pela Exato Soluções
            vars.put("e", extrato); // texto do extrato bancário
            vars.put("b", "conta bancária"); // conta bancária no formato banco-agência-conta
            vars.put("q", "sim"); // receber informações de quitação na resposta
            
            // variável opcional para o retorno já descompactado
            vars.put("fr", "txt");
            
            // chave da requisição (a inclusão da chave e o cálculo do MD5 são feitos automaticamente na chamada da requisição)
            String k = USWS + vars.get("c") + vars.get("b") + vars.get("e");
            
            // requisitando as informações
            resp = ws.requisitar("vdk-cartoes/bancario", CHWS, k, vars);
        }
        
        // envio de arquivo de extrato bancário
        if (servico.equals("extrato bancário")) {
            
            // carregando o texto do extrato bancário
            String extrato = ConciliadorJava.textoArquivo("caminho para arquivo cnab ou ofx");
            
            // variáveis de requisição
            vars.put("c", CLCONC); // identificador do cliente fornecido pela Exato Soluções
            vars.put("e", extrato); // texto do extrato bancário
            vars.put("b", "conta bancária"); // conta bancária no formato banco-agência-conta
            vars.put("m", "não"); // o extrato é um CNAB multi conta? (sim/não)
            
            // variável opcional para o retorno já descompactado
            vars.put("fr", "txt");
            
            // chave da requisição (a inclusão da chave e o cálculo do MD5 são feitos automaticamente na chamada da requisição)
            String k = USWS + vars.get("c") + vars.get("e");
            
            // requisitando as informações
            resp = ws.requisitar("vdk-cartoes/extrato-bancario", CHWS, k, vars);
        }
        
        // conferência de quitação do pagamento de parcelas
        if (servico.equals("quitação")) {
            
            // variáveis de requisição
            vars.put("c", CLCONC); // identificador do cliente fornecido pela Exato Soluções
            vars.put("d", "05/07/2022"); // data a consultar no formato DD/MM/AAAA ou AAAA-MM-DD
            vars.put("cn", "00000000000000"); // CNPJ da loja (não enviar ou deixar em branco para todas as do cliente)
            
            // variável opcional para o retorno já descompactado
            vars.put("fr", "txt");
            
            // chave da requisição (a inclusão da chave e o cálculo do MD5 são feitos automaticamente na chamada da requisição)
            String k = USWS + vars.get("c") + vars.get("d");
            
            // requisitando as informações
            resp = ws.requisitar("vdk-cartoes/quitacao", CHWS, k, vars);
        }
        
        // registrando o conteúdo da resposta
        try {
            PrintWriter saida = new PrintWriter("resposta.json");
            saida.println(resp);
            saida.close();
            System.out.println("O arquivo resposta.json com a resposta da consulta foi gravado.");
        } catch (Exception e) {
            System.out.println("Erro registrando o arquivo de resposta.");
        }
        
        // exibindo o log da requisição
        System.out.println("");
        System.out.println("LOG DE EXECUÇÃO");
        ArrayList<String> log = ws.recLog();
        for (int i=0; i<log.size(); i++) {
            System.out.println(log.get(i));
        }
    }
    
    /**
     * Recupera o texto de um arquivo.
     * @param caminho o caminho para o arquivo
     * @return o texto do arquivo ou string vazia caso ele não seja encontrado
     */
    public static String textoArquivo(String caminho)
    {
        String texto = "";
        try {
            texto = new String(Files.readAllBytes(Paths.get(caminho)));
        } catch (IOException e) {
            texto = "";
        }
        return(texto);
    }
    
}
