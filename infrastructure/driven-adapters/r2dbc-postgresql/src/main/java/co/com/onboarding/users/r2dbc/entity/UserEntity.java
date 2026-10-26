
package co.com.onboarding.users.r2dbc.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity {

    @Id
    private Long id;

    @Column("email")
    private String email;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("avatar")
    private String avatar;
}
