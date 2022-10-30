package controllers;

import java.util.ArrayList;

import models.Fornecedor;

public class FornecedorController {

	private static ArrayList<Fornecedor> fornecedores = new ArrayList<Fornecedor>();

	//Cadastrar
	public static void cadastrar(Fornecedor fornecedor) {
		fornecedores.add(fornecedor);
	}

	//Listar
	public static ArrayList<Fornecedor> listar() {
		return fornecedores;
	}

	//buscar por CNPJ
	public static Fornecedor buscarPorCnpj(String cnpj) {
		for(Fornecedor fornecedorCadastrado : fornecedores) {
			if(fornecedorCadastrado.getCnpj().equals(cnpj)) {
				return fornecedorCadastrado;
			}
		}
		return null;
	}

	//Editar produto
	public static Boolean editar (String cnpj, String novoCnpj, String novoNome, String novoTelefone, String novoEmail, String novoEndereco) {
		for(Fornecedor fornecedorCadastrado: fornecedores) {
			if(fornecedorCadastrado.getCnpj().equals(cnpj)) {
				if(novoCnpj != null){
					fornecedorCadastrado.setCnpj(novoCnpj);
				}
				if(novoNome != null){
					fornecedorCadastrado.setEmpresa(novoNome);
				}
				if(novoTelefone != null){
					fornecedorCadastrado.setTelefone(novoTelefone);
				}
				if(novoEmail != null){
					fornecedorCadastrado.setEmail(novoEmail);
				}
				if(novoEndereco != null){
					fornecedorCadastrado.setEndereço(novoEndereco);
				}
				return true;
			}
		}
		return false;
	}

	//Deletar fornecedor
	public static Boolean deletar(String cnpj){ 
		for(Fornecedor fornecedorCadastrado: fornecedores) {
			if(fornecedorCadastrado.getCnpj().equals(cnpj)) {
				fornecedores.remove(fornecedorCadastrado);
				return true;
			}
		}
		return false;
	}

}
