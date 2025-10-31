import domain.Users;

interface ApiController {
  /*
  идентификация пользователя
  @param email - почта пользователя
  @return есть ли пользователь с такой почтой
   */
  boolean identification(String email);

  /*
  аунтефикация пользователя
  @param password - пароль пользователя
  @return совпадает ли хеш пароля с хешом пароля индетифицированного пользователя
   */
  boolean authentication(String password);

  /*
  авторизация пользователя

  @return пользователь, который прошел авторизацию и аунтефикацию
   */
  Users authorization();


}