package views;

import controllers.FornecedorController;
import models.Fornecedor;

public class ListarFornecedores {


	public static void renderizar(){

		System.out.println("\n-- Lista de fornecedores --");

		for(Fornecedor fornecedorCadastrado: FornecedorController.listar()){
			System.out.println("\n");
			System.out.println("Criado em: "+fornecedorCadastrado.getCriadoEm());
			System.out.println("CNPJ: "+fornecedorCadastrado.getCnpj());
			System.out.println("Nome empresa: "+fornecedorCadastrado.getEmpresa());
			System.out.println("Telefone: "+fornecedorCadastrado.getTelefone());
			System.out.println("Email: "+fornecedorCadastrado.getEmail());
			System.out.println("Endereço: "+fornecedorCadastrado.getEndereço());
		}
	}
}
