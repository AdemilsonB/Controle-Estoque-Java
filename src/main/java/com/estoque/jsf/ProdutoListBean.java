package com.estoque.jsf;

import com.estoque.dto.response.ProdutoResponse;
import com.estoque.service.ProdutoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import java.io.Serializable;
import java.util.List;

/**
 * Bean CDI genuíno ({@code @Named} + {@code @ViewScoped} do próprio JSF, não do Spring) — o
 * {@link ProdutoService} do Spring é injetado via {@link SpringBeanAutowiringSupport}, ponte
 * padrão do Spring pra frameworks web fora do seu próprio container de DI.
 */
@Named
@ViewScoped
public class ProdutoListBean implements Serializable {

    private static final int TAMANHO_PAGINA = 10;

    private ProdutoService produtoService;
    private List<ProdutoResponse> produtos;
    private int paginaAtual;
    private boolean temProximaPagina;

    public ProdutoListBean() {
        // Construtor sem args exigido pelo CDI para proxy de @ViewScoped.
    }

    // Construtor usado pelo teste (injeção direta, sem passar pelo Spring).
    ProdutoListBean(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostConstruct
    public void iniciar() {
        if (produtoService == null) {
            // processInjectionBasedOnCurrentContext usa RequestContextHolder, que só é populado
            // em requisições que passam pelo DispatcherServlet — as do FacesServlet não passam.
            // Por isso pega o ServletContext direto do FacesContext, que sempre está disponível.
            jakarta.servlet.ServletContext servletContext =
                    (jakarta.servlet.ServletContext) jakarta.faces.context.FacesContext
                            .getCurrentInstance().getExternalContext().getContext();
            SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, servletContext);
        }
        paginaAtual = 0;
        carregarPagina();
    }

    // @Autowired do Spring, não @Inject do CDI: quem chama este setter é o
    // SpringBeanAutowiringSupport acima, por reflection direta — se fosse @Inject, o próprio
    // Weld tentaria resolver ProdutoService como bean CDI (que não é) e falharia o deploy antes
    // do @PostConstruct sequer rodar.
    @Autowired
    void setProdutoService(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    private void carregarPagina() {
        Page<ProdutoResponse> pagina = produtoService.listar(PageRequest.of(paginaAtual, TAMANHO_PAGINA));
        this.produtos = pagina.getContent();
        this.temProximaPagina = pagina.hasNext();
    }

    public void proximaPagina() {
        paginaAtual++;
        carregarPagina();
    }

    public void paginaAnterior() {
        if (paginaAtual > 0) {
            paginaAtual--;
            carregarPagina();
        }
    }

    public List<ProdutoResponse> getProdutos() {
        return produtos;
    }

    public int getPaginaAtual() {
        return paginaAtual;
    }

    public boolean isTemProximaPagina() {
        return temProximaPagina;
    }
}
